package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class ResumeProjectionServiceTest {

    @Mock private ResumeMongoRepository resumeMongoRepository;
    @Mock private ResumeMongoQueryRepository resumeMongoQueryRepository;
    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private MongoTemplate mongoTemplate;

    private ResumeProjectionService resumeProjectionService;

    @BeforeEach
    void setUp() {
        resumeProjectionService =
                new ResumeProjectionService(
                        resumeMongoRepository,
                        resumeMongoQueryRepository,
                        resumeEventMongoRepository,
                        mongoTemplate,
                        Clock.fixed(Instant.parse("2026-03-29T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createProjectionIfNoPendingOrThrow_whenUniqueConstraintTrips_throwsGenerationInProgress() {
        ResumeDocument projection = mock(ResumeDocument.class);
        ResumeEventDocument event = mock(ResumeEventDocument.class);

        given(resumeMongoRepository.existsByUserIdAndHasPendingWorkTrue(1L)).willReturn(false);
        given(resumeMongoRepository.save(projection))
                .willThrow(new DuplicateKeyException("pending unique violation"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                resumeProjectionService.createProjectionIfNoPendingOrThrow(
                                        1L, projection, event));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESUME_GENERATION_IN_PROGRESS);
        verify(resumeEventMongoRepository, never()).save(any());
    }

    @Test
    void
            createProjectionIfNoPendingOrThrow_whenUnexpectedDuplicateKeyTrips_throwsInternalServerError() {
        ResumeDocument projection = mock(ResumeDocument.class);
        ResumeEventDocument event = mock(ResumeEventDocument.class);

        given(resumeMongoRepository.existsByUserIdAndHasPendingWorkTrue(1L)).willReturn(false);
        given(resumeMongoRepository.save(projection))
                .willThrow(new DuplicateKeyException("ux_resumes_resume_id"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                resumeProjectionService.createProjectionIfNoPendingOrThrow(
                                        1L, projection, event));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        verify(resumeEventMongoRepository, never()).save(any());
    }

    @Test
    void markPendingIfIdleOrThrow_whenUniqueConstraintTrips_throwsEditInProgress() {
        given(mongoTemplate.findAndModify(any(), any(Update.class), eq(ResumeDocument.class)))
                .willThrow(new DuplicateKeyException("pending unique violation"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> resumeProjectionService.markPendingIfIdleOrThrow(100L, 1L));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESUME_EDIT_IN_PROGRESS);
    }

    @Test
    void commitVersionAndApplyProjection_whenCommittingLatestPreview_clearsUnseenPreview() {
        Instant committedAt = Instant.parse("2026-03-29T00:10:00Z");
        ResumeEventDocument event = mock(ResumeEventDocument.class);
        ResumeDocument projection = mock(ResumeDocument.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 3))
                .willReturn(Optional.of(event));
        given(resumeMongoRepository.findByResumeId(100L)).willReturn(Optional.of(projection));
        given(projection.getLatestPreviewVersionNo()).willReturn(3);

        resumeProjectionService.commitVersionAndApplyProjection(100L, 3, committedAt);

        verify(event).markPreviewShown(committedAt);
        verify(event).markCommitted(committedAt);
        verify(resumeEventMongoRepository).save(event);
        verify(mongoTemplate)
                .findAndModify(any(), updateCaptor.capture(), eq(ResumeDocument.class));
        Document setClause = updateCaptor.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(setClause).doesNotContainKey("has_unseen_preview");
        verify(resumeMongoQueryRepository).clearUnseenPreviewIfLatestVersion(100L, 3);
    }

    @Test
    void markPreviewShownAndClearUnseen_clearsOnlyMatchingLatestPreviewVersion() {
        Instant previewShownAt = Instant.parse("2026-03-29T00:10:00Z");
        ResumeEventDocument event = mock(ResumeEventDocument.class);

        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 3))
                .willReturn(Optional.of(event));

        resumeProjectionService.markPreviewShownAndClearUnseen(100L, 3, previewShownAt);

        verify(event).markPreviewShown(previewShownAt);
        verify(resumeEventMongoRepository).save(event);
        verify(resumeMongoQueryRepository).clearUnseenPreviewIfLatestVersion(100L, 3);
    }

    @Test
    void commitVersionAndApplyProjection_whenAlreadyCommitted_isNoOp() {
        Instant committedAt = Instant.parse("2026-03-29T00:10:00Z");
        ResumeEventDocument event = mock(ResumeEventDocument.class);
        ResumeDocument projection = mock(ResumeDocument.class);

        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 3))
                .willReturn(Optional.of(event));
        given(resumeMongoRepository.findByResumeId(100L)).willReturn(Optional.of(projection));
        given(projection.getLatestPreviewVersionNo()).willReturn(3);
        given(event.getCommittedAt()).willReturn(committedAt);

        resumeProjectionService.commitVersionAndApplyProjection(100L, 3, committedAt);

        verify(resumeEventMongoRepository, never()).save(any());
        verify(mongoTemplate, never())
                .findAndModify(any(), any(Update.class), eq(ResumeDocument.class));
        verify(resumeMongoQueryRepository, never()).clearUnseenPreviewIfLatestVersion(any(), any());
    }
}
