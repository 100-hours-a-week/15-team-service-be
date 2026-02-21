package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.sse.SseEmitterRegistry;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditFailedSsePayload;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditSsePayload;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeSseService {
    private final SseEmitterRegistry sseEmitterRegistry;

    public SseEmitter subscribe(Long resumeId) {
        SseEmitter emitter = sseEmitterRegistry.register(resumeId);

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            log.debug("[RESUME_SSE] connected_event resumeId={}", resumeId);
        } catch (IOException ex) {
            log.warn("[RESUME_SSE] connected_event_failed resumeId={}", resumeId, ex);
            sseEmitterRegistry.remove(resumeId, emitter);
        }

        return emitter;
    }

    public void sendEditCompleted(
            Long resumeId,
            Integer versionNo,
            String taskId,
            java.time.Instant updatedAt,
            Object resumePayload) {
        if (resumeId == null) {
            return;
        }
        List<SseEmitter> emitters = sseEmitterRegistry.getEmitters(resumeId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[RESUME_SSE] no_emitters resumeId={}", resumeId);
            return;
        }

        ResumeEditSsePayload payload =
                new ResumeEditSsePayload(resumeId, versionNo, taskId, updatedAt, resumePayload);

        log.debug(
                "[RESUME_SSE] send_edit_completed resumeId={} versionNo={} taskId={} emitters={}",
                resumeId,
                versionNo,
                taskId,
                emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("resume-edit-complete").data(payload));
            } catch (IOException ex) {
                log.warn(
                        "[RESUME_SSE] send_failed resumeId={} versionNo={} taskId={}",
                        resumeId,
                        versionNo,
                        taskId,
                        ex);
                sseEmitterRegistry.remove(resumeId, emitter);
            }
        }
    }

    public void sendEditFailed(
            Long resumeId,
            Integer versionNo,
            String taskId,
            java.time.Instant updatedAt,
            String errorCode,
            String errorMessage) {
        if (resumeId == null) {
            return;
        }
        List<SseEmitter> emitters = sseEmitterRegistry.getEmitters(resumeId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[RESUME_SSE] no_emitters resumeId={}", resumeId);
            return;
        }

        ResumeEditFailedSsePayload payload =
                new ResumeEditFailedSsePayload(
                        resumeId, versionNo, taskId, updatedAt, errorCode, errorMessage);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("resume-edit-failed").data(payload));
            } catch (IOException ex) {
                log.warn(
                        "[RESUME_SSE] send_failed resumeId={} versionNo={} taskId={}",
                        resumeId,
                        versionNo,
                        taskId,
                        ex);
                sseEmitterRegistry.remove(resumeId, emitter);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEditCompleted(ResumeEditCompletedEvent event) {
        if (event == null || event.content() == null) {
            return;
        }
        sendEditCompleted(
                event.resumeId(),
                event.versionNo(),
                event.taskId(),
                event.updatedAt(),
                event.content());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEditFailed(ResumeEditFailedEvent event) {
        if (event == null) {
            return;
        }
        sendEditFailed(
                event.resumeId(),
                event.versionNo(),
                event.taskId(),
                event.updatedAt(),
                event.errorCode(),
                event.errorMessage());
    }
}
