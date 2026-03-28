package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import com.sipomeokjo.commitme.domain.outbox.service.OutboxEventService;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeMapper;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.global.mongo.MongoSequenceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeServiceLockRoutingTest {

    @Mock private ResumeMongoRepository resumeMongoRepository;
    @Mock private ResumeMongoQueryRepository resumeMongoQueryRepository;
    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeFinder resumeFinder;
    @Mock private MongoSequenceService mongoSequenceService;
    @Mock private PositionFinder positionFinder;
    @Mock private CompanyRepository companyRepository;
    @Mock private CursorParser cursorParser;
    @Mock private ResumeMapper resumeMapper;
    @Mock private ResumeProfileService resumeProfileService;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private OutboxEventService outboxEventService;
    @Mock private ResumeAiRequestService resumeAiRequestService;
    @Mock private ResumeEditTransactionService resumeEditTransactionService;
    @Mock private ResumeProjectionService resumeProjectionService;
    @Mock private ResumeLockService resumeLockService;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeService =
                new ResumeService(
                        resumeMongoRepository,
                        resumeMongoQueryRepository,
                        resumeEventMongoRepository,
                        resumeFinder,
                        mongoSequenceService,
                        positionFinder,
                        companyRepository,
                        cursorParser,
                        resumeMapper,
                        resumeProfileService,
                        outboxEventRepository,
                        outboxEventService,
                        resumeAiRequestService,
                        resumeEditTransactionService,
                        resumeProjectionService,
                        resumeLockService,
                        Clock.fixed(Instant.parse("2026-03-27T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void create_whenRedisLockAcquired_usesPreAcquiredLockPath() {
        ResumeCreateRequest request = createRequest();
        given(positionFinder.getByIdOrThrow(3L))
                .willReturn(Position.builder().id(3L).name("Backend").build());
        given(resumeLockService.tryAcquireCreateLock(eq(11L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.ACQUIRED);
        given(mongoSequenceService.nextResumeId()).willReturn(100L);
        given(resumeLockService.bindCreateLockOwner(eq(11L), anyString(), eq(100L)))
                .willReturn(true);

        resumeService.create(11L, request);

        InOrder inOrder = inOrder(resumeLockService, mongoSequenceService);
        inOrder.verify(resumeLockService).tryAcquireCreateLock(eq(11L), anyString());
        inOrder.verify(mongoSequenceService).nextResumeId();
        verify(resumeLockService).bindCreateLockOwner(eq(11L), anyString(), eq(100L));
        verify(resumeProjectionService).createProjectionWithPreAcquiredLock(any(), any());
        verify(resumeProjectionService, never())
                .createProjectionIfNoPendingOrThrow(any(), any(), any());
    }

    @Test
    void create_whenRedisFallsBack_usesMongoGuardPath() {
        ResumeCreateRequest request = createRequest();
        given(positionFinder.getByIdOrThrow(3L))
                .willReturn(Position.builder().id(3L).name("Backend").build());
        given(resumeLockService.tryAcquireCreateLock(eq(11L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.FALLBACK);
        given(mongoSequenceService.nextResumeId()).willReturn(100L);

        resumeService.create(11L, request);

        verify(resumeProjectionService).createProjectionIfNoPendingOrThrow(eq(11L), any(), any());
        verify(resumeProjectionService, never()).createProjectionWithPreAcquiredLock(any(), any());
        verify(resumeLockService, never()).bindCreateLockOwner(eq(11L), anyString(), eq(100L));
    }

    @Test
    void create_whenRedisReportsBusy_throwsConflict() {
        ResumeCreateRequest request = createRequest();
        given(positionFinder.getByIdOrThrow(3L))
                .willReturn(Position.builder().id(3L).name("Backend").build());
        given(resumeLockService.tryAcquireCreateLock(eq(11L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.BUSY);

        assertThatThrownBy(() -> resumeService.create(11L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_GENERATION_IN_PROGRESS);

        verify(resumeProjectionService, never()).createProjectionWithPreAcquiredLock(any(), any());
        verify(resumeProjectionService, never())
                .createProjectionIfNoPendingOrThrow(any(), any(), any());
        verify(mongoSequenceService, never()).nextResumeId();
    }

    @Test
    void create_whenOutboxEnqueueFails_marksEventFailedAndClearsPending() {
        ResumeCreateRequest request = createRequest();
        ResumeEventDocument queued =
                ResumeEventDocument.create(100L, 1, 11L, ResumeVersionStatus.QUEUED, "{}");
        given(positionFinder.getByIdOrThrow(3L))
                .willReturn(Position.builder().id(3L).name("Backend").build());
        given(resumeLockService.tryAcquireCreateLock(eq(11L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.ACQUIRED);
        given(mongoSequenceService.nextResumeId()).willReturn(100L);
        given(resumeLockService.bindCreateLockOwner(eq(11L), anyString(), eq(100L)))
                .willReturn(true);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 1))
                .willReturn(Optional.of(queued));
        doThrow(new RuntimeException("enqueue failed"))
                .when(outboxEventService)
                .enqueue(any(), any(), any(), any());

        assertThatThrownBy(() -> resumeService.create(11L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("enqueue failed");

        verify(resumeEventMongoRepository).save(queued);
        verify(resumeProjectionService).applyAiFailure(100L, 1);
        verify(resumeLockService).releaseCreateLock(11L, 100L);
    }

    @Test
    void edit_whenRedisLockAcquired_usesPreAcquiredLockPath() {
        ResumeEditTransactionService.EditPrepared prepared =
                new ResumeEditTransactionService.EditPrepared(100L, "resume", 2, "{}");
        ResumeEventDocument updated =
                ResumeEventDocument.create(100L, 2, 11L, ResumeVersionStatus.QUEUED, "{}");
        updated.startProcessing("job-1", Instant.parse("2026-03-27T00:00:05Z"));

        given(resumeLockService.tryAcquireEditLock(eq(100L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.ACQUIRED);
        given(resumeEditTransactionService.peekNextVersionNo(100L)).willReturn(2);
        given(resumeLockService.bindEditLockOwner(eq(100L), anyString(), eq(2))).willReturn(true);
        given(resumeEditTransactionService.prepareEditWithPreAcquiredLock(11L, 100L, 2))
                .willReturn(prepared);
        given(resumeAiRequestService.requestEdit(100L, "{}", "update summary")).willReturn("job-1");
        given(resumeEditTransactionService.markEditRequested(100L, 2, "job-1")).willReturn(updated);

        resumeService.edit(11L, 100L, new ResumeEditRequest("update summary"));

        InOrder inOrder = inOrder(resumeLockService, resumeEditTransactionService);
        inOrder.verify(resumeLockService).tryAcquireEditLock(eq(100L), anyString());
        inOrder.verify(resumeEditTransactionService).peekNextVersionNo(100L);
        verify(resumeLockService).bindEditLockOwner(eq(100L), anyString(), eq(2));
        verify(resumeEditTransactionService).prepareEditWithPreAcquiredLock(11L, 100L, 2);
        verify(resumeEditTransactionService, never()).prepareEdit(11L, 100L);
    }

    @Test
    void edit_whenRedisFallsBack_usesMongoGuardPath() {
        ResumeEditTransactionService.EditPrepared prepared =
                new ResumeEditTransactionService.EditPrepared(100L, "resume", 2, "{}");
        ResumeEventDocument updated =
                ResumeEventDocument.create(100L, 2, 11L, ResumeVersionStatus.QUEUED, "{}");
        updated.startProcessing("job-1", Instant.parse("2026-03-27T00:00:05Z"));

        given(resumeLockService.tryAcquireEditLock(eq(100L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.FALLBACK);
        given(resumeEditTransactionService.prepareEdit(11L, 100L)).willReturn(prepared);
        given(resumeAiRequestService.requestEdit(100L, "{}", "update summary")).willReturn("job-1");
        given(resumeEditTransactionService.markEditRequested(100L, 2, "job-1")).willReturn(updated);

        resumeService.edit(11L, 100L, new ResumeEditRequest("update summary"));

        verify(resumeEditTransactionService).prepareEdit(11L, 100L);
        verify(resumeEditTransactionService, never()).prepareEditWithPreAcquiredLock(11L, 100L, 2);
        verify(resumeEditTransactionService, never()).peekNextVersionNo(100L);
    }

    @Test
    void edit_whenRedisReportsBusy_throwsConflict() {
        given(resumeLockService.tryAcquireEditLock(eq(100L), anyString()))
                .willReturn(ResumeLockService.LockAcquireResult.BUSY);

        assertThatThrownBy(
                        () ->
                                resumeService.edit(
                                        11L, 100L, new ResumeEditRequest("update summary")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_EDIT_IN_PROGRESS);

        verify(resumeEditTransactionService, never()).peekNextVersionNo(100L);
        verify(resumeEditTransactionService, never()).prepareEdit(11L, 100L);
        verify(resumeEditTransactionService, never()).prepareEditWithPreAcquiredLock(11L, 100L, 2);
    }

    @Test
    void get_whenPendingMetadataMissing_defaultsIsEditingToFalse() {
        ResumeDocument doc = org.mockito.Mockito.mock(ResumeDocument.class);
        ResumeEventDocument event =
                ResumeEventDocument.create(100L, 1, 11L, ResumeVersionStatus.SUCCEEDED, "{}");
        event.succeed("{}", Instant.parse("2026-03-27T00:00:05Z"));
        ResumeDetailDto expected =
                new ResumeDetailDto(
                        100L,
                        "resume",
                        false,
                        3L,
                        "Backend",
                        null,
                        null,
                        1,
                        "{}",
                        null,
                        null,
                        null,
                        Instant.parse("2026-03-27T00:00:05Z"));

        given(doc.getResumeId()).willReturn(100L);
        given(doc.getProfileSnapshot()).willReturn(null);
        given(doc.isHasPendingWork()).willReturn(false);
        given(doc.getCurrentVersionNo()).willReturn(1);
        given(resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(100L, 11L)).willReturn(doc);
        given(resumeProfileService.getProfile(11L, 100L, null)).willReturn(null);
        given(resumeEventMongoRepository.existsByResumeIdAndIsPendingTrue(100L)).willReturn(false);
        given(
                        resumeEventMongoRepository
                                .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                                        100L, ResumeVersionStatus.SUCCEEDED))
                .willReturn(Optional.empty());
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 1))
                .willReturn(Optional.of(event));
        given(resumeMapper.toDetailDtoFromDocument(doc, event, false, null)).willReturn(expected);

        resumeService.get(11L, 100L);

        verify(resumeMapper).toDetailDtoFromDocument(doc, event, false, null);
    }

    private ResumeCreateRequest createRequest() {
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setRepoUrls(List.of("https://github.com/commitme/repo"));
        request.setPositionId(3L);
        request.setName("resume");
        return request;
    }
}
