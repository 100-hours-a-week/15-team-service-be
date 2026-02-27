package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeTimeoutScheduler {
    private static final long AI_PROCESSING_TIMEOUT_MINUTES = 5;
    private static final long TIMEOUT_CHECK_INTERVAL_MS = 60_000L;

    private final ResumeVersionRepository resumeVersionRepository;

    @Scheduled(fixedDelay = TIMEOUT_CHECK_INTERVAL_MS)
    @Transactional
    public void failTimedOutVersions() {
        List<ResumeVersion> pendingVersions =
                resumeVersionRepository.findEntitiesByStatusIn(
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));

        int timeoutCount = 0;
        for (ResumeVersion v : pendingVersions) {
            if (v.isProcessingTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)
                    || v.isQueuedTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)) {
                v.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
                timeoutCount++;
            }
        }

        if (timeoutCount > 0) {
            log.info("[RESUME_TIMEOUT] failed_count={}", timeoutCount);
        }
    }
}
