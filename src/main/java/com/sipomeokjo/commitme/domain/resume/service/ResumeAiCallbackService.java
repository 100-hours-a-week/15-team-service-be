package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventTypes;
import com.sipomeokjo.commitme.domain.outbox.service.OutboxEventService;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCallbackSource;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCompletionOutboxPayload;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAiCallbackService {

    private final ResumeEventQueryRepository resumeEventQueryRepository;
    private final ResumeMongoRepository resumeMongoRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventService outboxEventService;
    private final ResumeProjectionService resumeProjectionService;

    @Transactional
    public void handleCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, ResumeCallbackSource.CREATE);
    }

    @Transactional
    public void handleEditCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, ResumeCallbackSource.EDIT);
    }

    private void handleCallbackInternal(
            AiResumeCallbackRequest req, ResumeCallbackSource callbackSource) {

        if (req == null || req.jobId() == null || req.jobId().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (req.status() == null || req.status().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Instant now = Instant.now();
        CallbackResolution resolution = resolveCallbackRequest(req);

        boolean isCreate = callbackSource == ResumeCallbackSource.CREATE;
        Instant committedAt =
                (isCreate && resolution.status() == ResumeVersionStatus.SUCCEEDED) ? now : null;

        resumeEventQueryRepository
                .transitionToTerminalIfPossible(
                        req.jobId(),
                        resolution.status(),
                        resolution.snapshot(),
                        resolution.errorLog(),
                        now,
                        committedAt)
                .ifPresentOrElse(
                        event -> {
                            applyProjection(event, isCreate);
                            publishCompletionEvent(buildResult(event, now), req, callbackSource);
                        },
                        () ->
                                log.debug(
                                        "[RESUME_AI_CALLBACK] skipped_already_terminal_or_not_found jobId={}",
                                        req.jobId()));
    }

    private void applyProjection(ResumeEventDocument event, boolean isCreate) {
        if (event.getStatus() == ResumeVersionStatus.SUCCEEDED) {
            resumeProjectionService.applyAiSuccess(
                    event.getResumeId(), event.getVersionNo(), isCreate);
        } else {
            resumeProjectionService.applyAiFailure(event.getResumeId(), event.getVersionNo());
        }
    }

    private CallbackResolution resolveCallbackRequest(AiResumeCallbackRequest req) {
        String statusStr = req.status().trim();

        if ("success".equalsIgnoreCase(statusStr)) {
            if (req.content() == null) {
                return new CallbackResolution(
                        ResumeVersionStatus.FAILED, null, "[BAD_CALLBACK] content is null");
            }
            try {
                String snapshot = objectMapper.writeValueAsString(req.content());
                return new CallbackResolution(ResumeVersionStatus.SUCCEEDED, snapshot, null);
            } catch (Exception e) {
                return new CallbackResolution(
                        ResumeVersionStatus.FAILED,
                        null,
                        "[JSON_SERIALIZATION_FAILED] " + e.getMessage());
            }
        }

        if ("failed".equalsIgnoreCase(statusStr)) {
            return new CallbackResolution(
                    ResumeVersionStatus.FAILED,
                    null,
                    "[" + resolveErrorCode(req) + "] " + resolveErrorMessage(req));
        }

        return new CallbackResolution(
                ResumeVersionStatus.FAILED, null, "[BAD_CALLBACK] invalid status=" + req.status());
    }

    private record CallbackResolution(
            ResumeVersionStatus status, String snapshot, String errorLog) {}

    private record CallbackResult(
            Long userId,
            Long resumeId,
            Integer versionNo,
            String taskId,
            ResumeVersionStatus status,
            Instant updatedAt,
            boolean updated) {}

    private CallbackResult buildResult(ResumeEventDocument event, Instant now) {
        return new CallbackResult(
                event.getUserId(),
                event.getResumeId(),
                event.getVersionNo(),
                event.getAiTaskId(),
                event.getStatus(),
                now,
                true);
    }

    private String resolveErrorCode(AiResumeCallbackRequest req) {
        return (req.error() == null || req.error().code() == null || req.error().code().isBlank())
                ? "AI_FAILED"
                : req.error().code();
    }

    private String resolveErrorMessage(AiResumeCallbackRequest req) {
        return (req.error() == null || req.error().message() == null)
                ? "unknown"
                : req.error().message();
    }

    private void publishCompletionEvent(
            CallbackResult result, AiResumeCallbackRequest req, ResumeCallbackSource source) {
        String errorCode = resolveErrorCode(req);
        String errorMessage = resolveErrorMessage(req);
        String outboxEventType =
                result.status() == ResumeVersionStatus.SUCCEEDED
                        ? OutboxEventTypes.AI_JOB_COMPLETED
                        : OutboxEventTypes.AI_JOB_FAILED;

        String resumeName =
                resumeMongoRepository
                        .findByResumeId(result.resumeId())
                        .map(ResumeDocument::getName)
                        .orElse("이력서");

        String message = buildNotificationMessage(source, result.status(), resumeName);
        outboxEventService.enqueue(
                outboxEventType,
                "RESUME",
                String.valueOf(result.resumeId()),
                new ResumeCompletionOutboxPayload(
                        result.userId(),
                        result.resumeId(),
                        result.versionNo(),
                        result.taskId(),
                        result.updatedAt(),
                        result.status(),
                        source,
                        errorCode,
                        errorMessage,
                        resumeName,
                        message));
        log.debug(
                "[RESUME_AI_CALLBACK] completion_event_enqueued userId={} resumeId={} versionNo={} status={} source={}",
                result.userId(),
                result.resumeId(),
                result.versionNo(),
                result.status(),
                source);
    }

    private String buildNotificationMessage(
            ResumeCallbackSource source, ResumeVersionStatus status, String resumeName) {
        if (status == ResumeVersionStatus.FAILED) {
            if (source == ResumeCallbackSource.EDIT) {
                return resumeName + " 이력서의 수정에 실패했습니다.";
            }
            return resumeName + " 이력서의 생성에 실패했습니다.";
        }
        if (source == ResumeCallbackSource.EDIT) {
            return resumeName + " 이력서의 수정이 완료되었습니다.";
        }
        return resumeName + " 이력서의 생성이 완료되었습니다.";
    }
}
