package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
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
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionSummaryDto;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeMapper;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventAggregationRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventAggregationResult.VersionListResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private ResumeMongoRepository resumeMongoRepository;
    @Mock private ResumeMongoQueryRepository resumeMongoQueryRepository;
    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeEventAggregationRepository resumeEventAggregationRepository;
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

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeService =
                new ResumeService(
                        resumeMongoRepository,
                        resumeMongoQueryRepository,
                        resumeEventMongoRepository,
                        resumeEventAggregationRepository,
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
                        Clock.fixed(Instant.parse("2026-03-29T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void create_whenOutboxEnqueueFails_marksInitialEventFailedAndClearsPending() {
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setRepoUrls(List.of("https://github.com/commit-me/repo"));
        request.setPositionId(10L);
        request.setName("백엔드 이력서");

        Position position = mock(Position.class);
        given(positionFinder.getByIdOrThrow(10L)).willReturn(position);
        given(position.getId()).willReturn(10L);
        given(position.getName()).willReturn("백엔드");
        given(mongoSequenceService.nextResumeId()).willReturn(100L);
        org.mockito.Mockito.doThrow(new IllegalStateException("enqueue failed"))
                .when(outboxEventService)
                .enqueue(anyString(), anyString(), anyString(), any());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> resumeService.create(1L, request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        verify(resumeProjectionService).applyCreateCompensation(100L, 1, "enqueue failed");
    }

    @Test
    void get_whenPreviewExists_marksPreviewShownViaProjectionTransaction() {
        ResumeDocument document = mock(ResumeDocument.class);
        ResumeEventDocument previewEvent =
                ResumeEventDocument.create(100L, 2, 1L, ResumeVersionStatus.QUEUED, "{}");
        previewEvent.succeed("{\"summary\":\"preview\"}", Instant.parse("2026-03-29T00:01:00Z"));

        ResumeProfileResponse profileResponse =
                new ResumeProfileResponse(
                        100L, "홍길동", null, null, null, null, List.of(), List.of(), List.of(),
                        List.of(), List.of());
        ResumeDetailDto detailDto =
                new ResumeDetailDto(
                        100L,
                        "이력서",
                        true,
                        10L,
                        "백엔드",
                        null,
                        null,
                        2,
                        "{\"summary\":\"preview\"}",
                        ResumeDetailDto.ResumeDetailProfileDto.from(profileResponse),
                        null,
                        Instant.parse("2026-03-29T00:00:00Z"),
                        Instant.parse("2026-03-29T00:00:00Z"));

        given(resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(100L, 1L)).willReturn(document);
        given(document.getResumeId()).willReturn(100L);
        given(document.getCurrentVersionNo()).willReturn(1);
        given(document.getProfileSnapshot()).willReturn(null);
        given(resumeProfileService.getProfile(1L, 100L, null)).willReturn(profileResponse);
        given(
                        resumeEventMongoRepository.existsByResumeIdAndStatusIn(
                                100L,
                                List.of(
                                        ResumeVersionStatus.QUEUED,
                                        ResumeVersionStatus.PROCESSING)))
                .willReturn(true);
        given(
                        resumeEventMongoRepository
                                .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                                        100L, ResumeVersionStatus.SUCCEEDED))
                .willReturn(Optional.of(previewEvent));
        given(resumeMapper.toDetailDtoFromDocument(document, previewEvent, true, profileResponse))
                .willReturn(detailDto);

        ResumeDetailDto result = resumeService.get(1L, 100L);

        assertThat(result).isSameAs(detailDto);
        verify(resumeProjectionService)
                .markPreviewShownAndClearUnseen(anyLong(), anyInt(), any(Instant.class));
        verify(resumeEventMongoRepository, never()).save(any());
    }

    @Test
    void getVersion_doesNotMutateTimedOutEventOnRead() {
        ResumeDocument document = mock(ResumeDocument.class);
        ResumeEventDocument event = mock(ResumeEventDocument.class);
        Instant createdAt = Instant.parse("2026-03-29T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-29T00:01:00Z");

        given(resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(100L, 1L)).willReturn(document);
        given(document.getResumeId()).willReturn(100L);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(event));
        given(event.getVersionNo()).willReturn(2);
        given(event.getStatus()).willReturn(ResumeVersionStatus.PROCESSING);
        given(event.getSnapshot()).willReturn("{\"summary\":\"processing\"}");
        given(event.getAiTaskId()).willReturn("task-1");
        given(event.getErrorLog()).willReturn(null);
        given(event.getStartedAt()).willReturn(createdAt);
        given(event.getFinishedAt()).willReturn(null);
        given(event.getCommittedAt()).willReturn(null);
        given(event.getCreatedAt()).willReturn(createdAt);
        given(event.getUpdatedAt()).willReturn(updatedAt);

        ResumeVersionDto result = resumeService.getVersion(1L, 100L, 2);

        assertThat(result.getStatus()).isEqualTo(ResumeVersionStatus.PROCESSING);
        verify(resumeEventMongoRepository, never()).save(any());
        verify(resumeProjectionService, never()).applyAiFailure(anyLong(), anyInt());
    }

    @Test
    void saveVersion_commitsThroughProjectionTransaction() {
        ResumeEventDocument event = mock(ResumeEventDocument.class);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(event));
        given(event.getStatus()).willReturn(ResumeVersionStatus.SUCCEEDED);

        resumeService.saveVersion(1L, 100L, 2);

        verify(resumeProjectionService)
                .commitVersionAndApplyProjection(anyLong(), anyInt(), any(Instant.class));
        verify(resumeEventMongoRepository, never()).save(any());
    }

    @Test
    void getVersionList_whenPreviewExists_doesNotExceedRequestedLimit() {
        ResumeEventDocument committedV3 =
                ResumeEventDocument.create(100L, 3, 1L, ResumeVersionStatus.QUEUED, "{}");
        committedV3.succeed("{}", Instant.parse("2026-03-29T00:03:00Z"));
        committedV3.markCommitted(Instant.parse("2026-03-29T00:03:00Z"));

        ResumeEventDocument committedV2 =
                ResumeEventDocument.create(100L, 2, 1L, ResumeVersionStatus.QUEUED, "{}");
        committedV2.succeed("{}", Instant.parse("2026-03-29T00:02:00Z"));
        committedV2.markCommitted(Instant.parse("2026-03-29T00:02:00Z"));

        ResumeEventDocument previewV4 =
                ResumeEventDocument.create(100L, 4, 1L, ResumeVersionStatus.QUEUED, "{}");
        previewV4.succeed("{}", Instant.parse("2026-03-29T00:04:00Z"));

        given(resumeEventAggregationRepository.findVersionListPage(100L, null, 2))
                .willReturn(new VersionListResult(List.of(committedV3, committedV2), previewV4));

        CursorResponse<ResumeVersionSummaryDto> response =
                resumeService.getVersionList(1L, 100L, new CursorRequest(null, 2));

        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).versionNo()).isEqualTo(4);
        assertThat(response.data().get(1).versionNo()).isEqualTo(3);
        assertThat(response.next()).isEqualTo("3");
    }

    @Test
    void getVersionList_whenPreviewExistsAndSizeIsOne_returnsNextCursorFromCommittedEvents() {
        ResumeEventDocument committedV3 =
                ResumeEventDocument.create(100L, 3, 1L, ResumeVersionStatus.QUEUED, "{}");
        committedV3.succeed("{}", Instant.parse("2026-03-29T00:03:00Z"));
        committedV3.markCommitted(Instant.parse("2026-03-29T00:03:00Z"));

        ResumeEventDocument previewV4 =
                ResumeEventDocument.create(100L, 4, 1L, ResumeVersionStatus.QUEUED, "{}");
        previewV4.succeed("{}", Instant.parse("2026-03-29T00:04:00Z"));

        given(resumeEventAggregationRepository.findVersionListPage(100L, null, 1))
                .willReturn(new VersionListResult(List.of(committedV3), previewV4));

        CursorResponse<ResumeVersionSummaryDto> response =
                resumeService.getVersionList(1L, 100L, new CursorRequest(null, 1));

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().versionNo()).isEqualTo(4);
        assertThat(response.next()).isEqualTo("3");
    }

    @Test
    void edit_whenMarkEditRequestedFailsAfterAiAccepted_doesNotMarkEditFailed() {
        ResumeEditRequest request = new ResumeEditRequest("문장 다듬어줘");
        ResumeEditTransactionService.EditPrepared prepared =
                new ResumeEditTransactionService.EditPrepared(100L, "이력서", 2, "{}");

        given(resumeEditTransactionService.prepareEdit(1L, 100L)).willReturn(prepared);
        given(resumeAiRequestService.requestEdit(100L, "{}", "문장 다듬어줘")).willReturn("task-1");
        given(resumeEditTransactionService.markEditRequested(100L, 2, "task-1"))
                .willThrow(new IllegalStateException("mongo write failed"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> resumeService.edit(1L, 100L, request));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        verify(resumeEditTransactionService, never())
                .markEditFailed(anyLong(), anyInt(), anyString());
    }

    @Test
    void saveVersion_whenAlreadyCommitted_isIdempotentNoOp() {
        ResumeEventDocument event = mock(ResumeEventDocument.class);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(event));
        given(event.getStatus()).willReturn(ResumeVersionStatus.SUCCEEDED);
        given(event.getCommittedAt()).willReturn(Instant.parse("2026-03-29T00:10:00Z"));

        resumeService.saveVersion(1L, 100L, 2);

        verify(resumeProjectionService, never())
                .commitVersionAndApplyProjection(anyLong(), anyInt(), any(Instant.class));
    }
}
