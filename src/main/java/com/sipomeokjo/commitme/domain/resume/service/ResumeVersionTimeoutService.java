package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeVersionTimeoutService {
    public static final long AI_PROCESSING_TIMEOUT_MINUTES = 5;
    private static final List<ResumeVersionStatus> PENDING_STATUSES =
            List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING);

    private final ResumeVersionRepository resumeVersionRepository;

    @Scheduled(fixedDelayString = "${app.resume.timeout-sweep-delay-ms:60000}")
    @Transactional
    public void sweepTimeoutVersions() {
        List<ResumeVersion> pendingVersions =
                resumeVersionRepository.findEntitiesByStatusIn(PENDING_STATUSES);
        int timeoutCount = 0;
        for (ResumeVersion version : pendingVersions) {
            if (version.isProcessingTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)
                    || version.isQueuedTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)) {
                version.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
                timeoutCount++;
            }
        }
        if (timeoutCount > 0) {
            log.info("[RESUME_TIMEOUT] timeout_failed count={}", timeoutCount);
        }
    }
}
