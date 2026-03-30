package com.sipomeokjo.commitme.domain.resume.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventTypes;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeCreateRepairServiceTest {

    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ResumeProjectionService resumeProjectionService;

    private ResumeCreateRepairService resumeCreateRepairService;

    @BeforeEach
    void setUp() {
        resumeCreateRepairService =
                new ResumeCreateRepairService(
                        resumeEventMongoRepository,
                        outboxEventRepository,
                        resumeProjectionService,
                        Clock.fixed(Instant.parse("2026-03-29T00:10:00Z"), ZoneOffset.UTC));
        ReflectionTestUtils.setField(resumeCreateRepairService, "repairGraceMinutes", 2L);
    }

    @Test
    void repairQueuedCreatesMissingOutbox_marksEventFailedAndClearsPending() {
        ResumeEventDocument event =
                ResumeEventDocument.create(100L, 1, 1L, ResumeVersionStatus.QUEUED, "{}");
        given(
                        resumeEventMongoRepository
                                .findByVersionNoAndStatusAndAiTaskIdIsNullAndCreatedAtBefore(
                                        1,
                                        ResumeVersionStatus.QUEUED,
                                        Instant.parse("2026-03-29T00:08:00Z")))
                .willReturn(List.of(event));
        given(
                        outboxEventRepository.existsByEventTypeAndAggregateTypeAndAggregateId(
                                OutboxEventTypes.AI_JOB_REQUESTED, "RESUME_EVENT", "100"))
                .willReturn(false);

        resumeCreateRepairService.repairQueuedCreatesMissingOutbox();

        verify(resumeEventMongoRepository).save(event);
        verify(resumeProjectionService).applyAiFailure(100L, 1);
    }

    @Test
    void repairQueuedCreatesMissingOutbox_skipsWhenOutboxExists() {
        ResumeEventDocument event =
                ResumeEventDocument.create(100L, 1, 1L, ResumeVersionStatus.QUEUED, "{}");
        given(
                        resumeEventMongoRepository
                                .findByVersionNoAndStatusAndAiTaskIdIsNullAndCreatedAtBefore(
                                        1,
                                        ResumeVersionStatus.QUEUED,
                                        Instant.parse("2026-03-29T00:08:00Z")))
                .willReturn(List.of(event));
        given(
                        outboxEventRepository.existsByEventTypeAndAggregateTypeAndAggregateId(
                                OutboxEventTypes.AI_JOB_REQUESTED, "RESUME_EVENT", "100"))
                .willReturn(true);

        resumeCreateRepairService.repairQueuedCreatesMissingOutbox();

        verify(resumeEventMongoRepository, never()).save(event);
        verify(resumeProjectionService, never()).applyAiFailure(100L, 1);
    }
}
