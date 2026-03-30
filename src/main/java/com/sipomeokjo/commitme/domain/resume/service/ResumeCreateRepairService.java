package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventTypes;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeCreateRepairService {
    private static final String RESUME_EVENT_AGGREGATE_TYPE = "RESUME_EVENT";

    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ResumeProjectionService resumeProjectionService;
    private final Clock clock;

    @Value("${app.resume.create-repair-grace-minutes:2}")
    private long repairGraceMinutes;

    @Scheduled(fixedDelayString = "${app.resume.create-repair-delay-ms:60000}")
    public void repairQueuedCreatesMissingOutbox() {
        Instant cutoff = Instant.now(clock).minus(repairGraceMinutes, ChronoUnit.MINUTES);

        List<ResumeEventDocument> orphanCandidates =
                resumeEventMongoRepository
                        .findByVersionNoAndStatusAndAiTaskIdIsNullAndCreatedAtBefore(
                                1, ResumeVersionStatus.QUEUED, cutoff);

        int repairedCount = 0;
        for (ResumeEventDocument event : orphanCandidates) {
            boolean hasOutbox =
                    outboxEventRepository.existsByEventTypeAndAggregateTypeAndAggregateId(
                            OutboxEventTypes.AI_JOB_REQUESTED,
                            RESUME_EVENT_AGGREGATE_TYPE,
                            String.valueOf(event.getResumeId()));
            if (hasOutbox) {
                continue;
            }
            event.failNow("OUTBOX_MISSING", "AI 요청 outbox 이벤트를 찾을 수 없습니다.");
            resumeEventMongoRepository.save(event);
            resumeProjectionService.applyAiFailure(event.getResumeId(), event.getVersionNo());
            repairedCount++;
        }

        if (repairedCount > 0) {
            log.warn("[RESUME_CREATE_REPAIR] repaired_missing_outbox count={}", repairedCount);
        }
    }
}
