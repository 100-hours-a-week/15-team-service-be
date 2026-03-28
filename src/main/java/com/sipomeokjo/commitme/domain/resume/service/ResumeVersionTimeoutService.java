package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeVersionTimeoutService {
    public static final long AI_PROCESSING_TIMEOUT_MINUTES = 5;
    private static final List<ResumeVersionStatus> PENDING_STATUSES =
            List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING);

    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeProjectionService resumeProjectionService;

    @Scheduled(fixedDelayString = "${app.resume.timeout-sweep-delay-ms:60000}")
    public void sweepTimeoutVersions() {
        List<ResumeEventDocument> pendingEvents =
                resumeEventMongoRepository.findByStatusIn(PENDING_STATUSES);
        int timeoutCount = 0;
        for (ResumeEventDocument event : pendingEvents) {
            if (event.isProcessingTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)
                    || event.isQueuedTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)) {
                event.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
                resumeEventMongoRepository.save(event);
                resumeProjectionService.applyAiFailure(event.getResumeId(), event.getVersionNo());
                timeoutCount++;
            }
        }
        if (timeoutCount > 0) {
            log.info("[RESUME_TIMEOUT] timeout_failed count={}", timeoutCount);
        }
    }
}
