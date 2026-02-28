package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCallbackSource;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCompletionEvent;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeAiCallbackService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void handleCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, true, ResumeCallbackSource.CREATE);
    }

    public void handleEditCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, true, ResumeCallbackSource.EDIT);
    }

    private void handleCallbackInternal(
            AiResumeCallbackRequest req,
            boolean publishCompletionEvent,
            ResumeCallbackSource callbackSource) {

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
            if (publishCompletionEvent) {
                publishCompletionEvent(result, req, callbackSource);
            }
            return;
        }

        String status = req.status().trim();

        if ("success".equalsIgnoreCase(status)) {
            if (req.content() == null) {
                version.failNow("BAD_CALLBACK", "content is null");
                ResumeAiCallbackResult result = toResult(version, true);
                if (publishCompletionEvent) {
                    publishCompletionEvent(result, req, callbackSource);
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
            if (publishCompletionEvent) {
                publishCompletionEvent(result, req, callbackSource);
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
            if (publishCompletionEvent) {
                publishCompletionEvent(result, req, callbackSource);
            }
            return;
        }

        version.failNow("BAD_CALLBACK", "invalid status=" + req.status());
        ResumeAiCallbackResult result = toResult(version, true);
        if (publishCompletionEvent) {
            publishCompletionEvent(result, req, callbackSource);
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

    private void publishCompletionEvent(
            ResumeAiCallbackResult result,
            AiResumeCallbackRequest req,
            ResumeCallbackSource source) {
        if (result == null) {
            return;
        }
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
                new ResumeCompletionEvent(
                        result.userId(),
                        result.resumeId(),
                        result.versionNo(),
                        result.taskId(),
                        result.updatedAt(),
                        result.status(),
                        source,
                        errorCode,
                        errorMessage));
        log.debug(
                "[RESUME_AI_CALLBACK] completion_event_published userId={} resumeId={} versionNo={} status={} source={}",
                result.userId(),
                result.resumeId(),
                result.versionNo(),
                result.status(),
                source);
    }
}
