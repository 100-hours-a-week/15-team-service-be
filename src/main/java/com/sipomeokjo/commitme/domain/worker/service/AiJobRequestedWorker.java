package com.sipomeokjo.commitme.domain.worker.service;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiRequestService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiRequestService.DispatchResult;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobRequestedPayload;
import com.sipomeokjo.commitme.domain.worker.repository.ResumeVersionWorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobRequestedWorker {
    private final ResumeVersionWorkerRepository resumeVersionWorkerRepository;
    private final ResumeAiRequestService resumeAiRequestService;
    private final TransactionTemplate transactionTemplate;

    public WorkerHandleResult handle(AiJobRequestedPayload payload) {
        if (payload == null
                || payload.resumeVersionId() == null
                || payload.userId() == null
                || payload.positionName() == null
                || payload.positionName().isBlank()
                || payload.repoUrls() == null
                || payload.repoUrls().isEmpty()) {
            return WorkerHandleResult.invalid("payload_missing_required_field");
        }

        String precheckFailureReason = precheckWithLock(payload);
        if (precheckFailureReason != null) {
            return WorkerHandleResult.skipped(precheckFailureReason);
        }

        DispatchResult dispatchResult =
                resumeAiRequestService.requestGenerateJob(
                        payload.userId(), payload.positionName(), payload.repoUrls());
        applyDispatchResultWithLock(payload.resumeVersionId(), dispatchResult);
        log.debug(
                "[AI_JOB_REQUESTED_WORKER] handled resumeVersionId={} userId={}",
                payload.resumeVersionId(),
                payload.userId());
        return WorkerHandleResult.success();
    }

    private String precheckWithLock(AiJobRequestedPayload payload) {
        return transactionTemplate.execute(
                status -> {
                    ResumeVersion version =
                            resumeVersionWorkerRepository
                                    .findByIdWithPessimisticWrite(payload.resumeVersionId())
                                    .orElse(null);
                    if (version == null) {
                        return "resume_version_not_found";
                    }

                    if (!payload.userId().equals(version.getResume().getUser().getId())) {
                        return "resume_version_user_mismatch";
                    }

                    if (version.getStatus() != ResumeVersionStatus.QUEUED) {
                        return "resume_version_not_queued";
                    }
                    return null;
                });
    }

    private void applyDispatchResultWithLock(Long resumeVersionId, DispatchResult dispatchResult) {
        transactionTemplate.executeWithoutResult(
                status -> {
                    ResumeVersion version =
                            resumeVersionWorkerRepository
                                    .findByIdWithPessimisticWrite(resumeVersionId)
                                    .orElse(null);
                    if (version == null || version.getStatus() != ResumeVersionStatus.QUEUED) {
                        return;
                    }

                    if (dispatchResult.success()) {
                        version.startProcessing(dispatchResult.jobId());
                        return;
                    }

                    version.failNow(dispatchResult.errorCode(), dispatchResult.errorMessage());
                });
    }
}
