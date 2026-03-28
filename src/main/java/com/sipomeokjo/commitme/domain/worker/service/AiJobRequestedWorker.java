package com.sipomeokjo.commitme.domain.worker.service;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiRequestService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiRequestService.DispatchResult;
import com.sipomeokjo.commitme.domain.resume.service.ResumeLockService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProjectionService;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobRequestedPayload;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobRequestedWorker {
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeAiRequestService resumeAiRequestService;
    private final ResumeProjectionService resumeProjectionService;
    private final ResumeLockService resumeLockService;

    public WorkerHandleResult handle(AiJobRequestedPayload payload) {
        if (payload == null
                || payload.resumeId() == null
                || payload.versionNo() == null
                || payload.userId() == null
                || payload.positionName() == null
                || payload.positionName().isBlank()
                || payload.repoUrls() == null
                || payload.repoUrls().isEmpty()) {
            return WorkerHandleResult.invalid("payload_missing_required_field");
        }

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(payload.resumeId(), payload.versionNo())
                        .orElse(null);

        if (event == null) {
            return WorkerHandleResult.skipped("resume_event_not_found");
        }

        if (!payload.userId().equals(event.getUserId())) {
            return WorkerHandleResult.skipped("resume_event_user_mismatch");
        }

        if (event.getStatus() != ResumeVersionStatus.QUEUED) {
            return WorkerHandleResult.skipped("resume_event_not_queued");
        }

        DispatchResult dispatchResult =
                resumeAiRequestService.requestGenerateJob(
                        payload.userId(), payload.positionName(), payload.repoUrls());

        ResumeEventDocument fresh =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(payload.resumeId(), payload.versionNo())
                        .orElse(null);

        if (fresh == null || fresh.getStatus() != ResumeVersionStatus.QUEUED) {
            return WorkerHandleResult.skipped("resume_event_state_changed");
        }

        if (dispatchResult.success()) {
            fresh.startProcessing(dispatchResult.jobId(), Instant.now());
        } else {
            fresh.fail(dispatchResult.errorMessage(), Instant.now());
        }
        resumeEventMongoRepository.save(fresh);
        if (!dispatchResult.success()) {
            resumeProjectionService.applyAiFailure(payload.resumeId(), payload.versionNo());
            resumeLockService.releaseCreateLock(payload.userId(), payload.resumeId());
        }

        log.debug(
                "[AI_JOB_REQUESTED_WORKER] handled resumeId={} versionNo={} userId={}",
                payload.resumeId(),
                payload.versionNo(),
                payload.userId());
        return WorkerHandleResult.success();
    }
}
