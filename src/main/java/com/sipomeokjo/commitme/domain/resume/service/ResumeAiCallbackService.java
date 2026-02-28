package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeAiCallbackService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void handleCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, false);
    }

    public void handleEditCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, true);
    }

    private void handleCallbackInternal(AiResumeCallbackRequest req, boolean publishEvent) {

        if (req == null || req.jobId() == null || req.jobId().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (req.status() == null || req.status().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        ResumeVersion version =
                resumeVersionRepository
                        .findByAiTaskId(req.jobId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (version.getStatus() == ResumeVersionStatus.SUCCEEDED
                || version.getStatus() == ResumeVersionStatus.FAILED) {
            ResumeAiCallbackResult result = toResult(version, false);
            if (publishEvent) {
                publishSseEvent(result, req);
            }
            return;
        }

        String status = req.status().trim();

        if ("success".equalsIgnoreCase(status)) {
            if (req.content() == null) {
                version.failNow("BAD_CALLBACK", "content is null");
                ResumeAiCallbackResult result = toResult(version, true);
                if (publishEvent) {
                    publishSseEvent(result, req);
                }
                return;
            }

            try {
                String json = objectMapper.writeValueAsString(req.content());
                version.succeed(json);
            } catch (Exception e) {
                version.failNow("JSON_SERIALIZATION_FAILED", e.getMessage());
            }
            ResumeAiCallbackResult result = toResult(version, true);
            if (publishEvent) {
                publishSseEvent(result, req);
            }
            return;
        }

        if ("failed".equalsIgnoreCase(status)) {
            String code =
                    (req.error() == null
                                    || req.error().code() == null
                                    || req.error().code().isBlank())
                            ? "AI_FAILED"
                            : req.error().code();
            String msg =
                    (req.error() == null || req.error().message() == null)
                            ? "unknown"
                            : req.error().message();

            version.failNow(code, msg);
            ResumeAiCallbackResult result = toResult(version, true);
            if (publishEvent) {
                publishSseEvent(result, req);
            }
            return;
        }

        version.failNow("BAD_CALLBACK", "invalid status=" + req.status());
        ResumeAiCallbackResult result = toResult(version, true);
        if (publishEvent) {
            publishSseEvent(result, req);
        }
    }

    private ResumeAiCallbackResult toResult(ResumeVersion version, boolean updated) {
        return new ResumeAiCallbackResult(
                version.getResume().getUser().getId(),
                version.getResume().getId(),
                version.getVersionNo(),
                version.getAiTaskId(),
                version.getStatus(),
                version.getUpdatedAt(),
                updated);
    }

    private void publishSseEvent(ResumeAiCallbackResult result, AiResumeCallbackRequest req) {
        if (result == null) {
            return;
        }
        if (result.status() == ResumeVersionStatus.SUCCEEDED) {
            eventPublisher.publishEvent(
                    new ResumeEditCompletedEvent(
                            result.userId(),
                            result.resumeId(),
                            result.versionNo(),
                            result.taskId(),
                            result.updatedAt()));
            return;
        }
        if (result.status() == ResumeVersionStatus.FAILED) {
            String errorCode =
                    (req == null
                                    || req.error() == null
                                    || req.error().code() == null
                                    || req.error().code().isBlank())
                            ? "AI_FAILED"
                            : req.error().code();
            String errorMessage =
                    (req == null || req.error() == null || req.error().message() == null)
                            ? "unknown"
                            : req.error().message();
            eventPublisher.publishEvent(
                    new ResumeEditFailedEvent(
                            result.userId(),
                            result.resumeId(),
                            result.versionNo(),
                            result.taskId(),
                            result.updatedAt(),
                            errorCode,
                            errorMessage));
        }
    }
}
