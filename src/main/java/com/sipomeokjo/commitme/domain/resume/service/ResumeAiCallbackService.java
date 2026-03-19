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
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeAiCallbackService {

    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeMongoRepository resumeMongoRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventService outboxEventService;

    public void handleCallback(AiResumeCallbackRequest req) {
        handleCallbackInternal(req, ResumeCallbackSource.CREATE);
    }

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

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByAiTaskId(req.jobId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (isTerminal(event.getStatus())) {
            log.debug(
                    "[RESUME_AI_CALLBACK] already_terminal resumeId={} versionNo={} status={}",
                    event.getResumeId(),
                    event.getVersionNo(),
                    event.getStatus());
            return;
        }

        Instant now = Instant.now();
        applyCallbackResult(event, req, now);

        if (callbackSource == ResumeCallbackSource.CREATE
                && event.getStatus() == ResumeVersionStatus.SUCCEEDED) {
            event.markCommitted(now);
        }
        publishCompletionEvent(buildResult(event, now), req, callbackSource);
        resumeEventMongoRepository.save(event);
    }

    private void applyCallbackResult(
            ResumeEventDocument event, AiResumeCallbackRequest req, Instant now) {
        String status = req.status().trim();

        if ("success".equalsIgnoreCase(status)) {
            if (req.content() == null) {
                event.failNow("BAD_CALLBACK", "content is null");
                return;
            }
            try {
                event.succeed(objectMapper.writeValueAsString(req.content()), now);
            } catch (Exception e) {
                event.failNow("JSON_SERIALIZATION_FAILED", e.getMessage());
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
            event.failNow(code, msg);
            return;
        }

        event.failNow("BAD_CALLBACK", "invalid status=" + req.status());
    }

    private ResumeAiCallbackResult buildResult(ResumeEventDocument event, Instant now) {
        return new ResumeAiCallbackResult(
                event.getUserId(),
                event.getResumeId(),
                event.getVersionNo(),
                event.getAiTaskId(),
                event.getStatus(),
                now,
                true);
    }

    private boolean isTerminal(ResumeVersionStatus status) {
        return status == ResumeVersionStatus.SUCCEEDED || status == ResumeVersionStatus.FAILED;
    }

    private void publishCompletionEvent(
            ResumeAiCallbackResult result,
            AiResumeCallbackRequest req,
            ResumeCallbackSource source) {
        String errorCode =
                (req.error() == null || req.error().code() == null || req.error().code().isBlank())
                        ? "AI_FAILED"
                        : req.error().code();
        String errorMessage =
                (req.error() == null || req.error().message() == null)
                        ? "unknown"
                        : req.error().message();
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
