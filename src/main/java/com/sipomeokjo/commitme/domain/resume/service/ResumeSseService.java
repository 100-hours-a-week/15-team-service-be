package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.sse.SseEmitterRegistry;
import com.sipomeokjo.commitme.api.sse.SseExceptionUtils;
import com.sipomeokjo.commitme.api.sse.SseStreamKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryBus;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryEnvelope;
import com.sipomeokjo.commitme.api.sse.distributed.SseInstanceIdProvider;
import com.sipomeokjo.commitme.api.sse.distributed.SseLocalDeliveryHandler;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteRepository;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditFailedSsePayload;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditSsePayload;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import java.time.Duration;
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
public class ResumeSseService implements SseLocalDeliveryHandler {
    private static final String STREAM_TYPE_RESUME = "resume";
    private static final String EVENT_EDIT_COMPLETE = "resume-edit-complete";
    private static final String EVENT_EDIT_FAILED = "resume-edit-failed";
    private static final Duration ROUTE_TTL = Duration.ofMinutes(2);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;
    private final SseRouteRepository sseRouteRepository;
    private final SseDeliveryBus sseDeliveryBus;
    private final SseInstanceIdProvider sseInstanceIdProvider;

    public SseEmitter subscribe(Long resumeId) {
        SseStreamKey streamKey = streamKey(resumeId);
        SseEmitter emitter = sseEmitterRegistry.register(streamKey);
        refreshRouteTtl(streamKey);

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            log.debug("[RESUME_SSE] connected_event resumeId={}", resumeId);
        } catch (Exception ex) {
            if (SseExceptionUtils.isClientDisconnected(ex)) {
                log.debug("[RESUME_SSE] connected_event_client_disconnected resumeId={}", resumeId);
            } else {
                log.warn("[RESUME_SSE] connected_event_failed resumeId={}", resumeId, ex);
            }
            sseEmitterRegistry.completeWithError(streamKey, emitter, ex);
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
        ResumeEditSsePayload payload =
                new ResumeEditSsePayload(resumeId, versionNo, taskId, updatedAt, resumePayload);
        sendDistributed(resumeId, EVENT_EDIT_COMPLETE, payload, versionNo, taskId);
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
        ResumeEditFailedSsePayload payload =
                new ResumeEditFailedSsePayload(
                        resumeId, versionNo, taskId, updatedAt, errorCode, errorMessage);
        sendDistributed(resumeId, EVENT_EDIT_FAILED, payload, versionNo, taskId);
    }

    @Override
    public String streamType() {
        return STREAM_TYPE_RESUME;
    }

    @Override
    public void deliver(SseDeliveryEnvelope envelope) {
        if (envelope == null) {
            return;
        }

        Long resumeId = parseResumeId(envelope.streamKey());
        if (resumeId == null) {
            return;
        }
        if (envelope.data() == null) {
            return;
        }

        try {
            if (EVENT_EDIT_COMPLETE.equals(envelope.eventName())) {
                ResumeEditSsePayload payload =
                        objectMapper.treeToValue(envelope.data(), ResumeEditSsePayload.class);
                sendLocalOnly(
                        resumeId,
                        EVENT_EDIT_COMPLETE,
                        payload,
                        payload.versionNo(),
                        payload.taskId());
                return;
            }
            if (EVENT_EDIT_FAILED.equals(envelope.eventName())) {
                ResumeEditFailedSsePayload payload =
                        objectMapper.treeToValue(envelope.data(), ResumeEditFailedSsePayload.class);
                sendLocalOnly(
                        resumeId,
                        EVENT_EDIT_FAILED,
                        payload,
                        payload.versionNo(),
                        payload.taskId());
                return;
            }
        } catch (Exception ex) {
            log.warn(
                    "[RESUME_SSE] remote_payload_parse_failed resumeId={} eventName={}",
                    resumeId,
                    envelope.eventName(),
                    ex);
        }
    }

    private SseStreamKey streamKey(Long resumeId) {
        return SseStreamKey.of(STREAM_TYPE_RESUME, resumeId);
    }

    private void sendDistributed(
            Long resumeId, String eventName, Object payload, Integer versionNo, String taskId) {
        SseStreamKey localStreamKey = streamKey(resumeId);
        SseRouteKey routeKey = routeKey(resumeId);
        String localInstanceId = sseInstanceIdProvider.getInstanceId();

        java.util.Set<String> instanceIds;
        try {
            instanceIds = sseRouteRepository.findInstanceIds(routeKey);
        } catch (Exception ex) {
            log.warn("[RESUME_SSE] route_lookup_failed resumeId={}", resumeId, ex);
            sendLocalOnly(resumeId, eventName, payload, versionNo, taskId);
            return;
        }

        if (instanceIds.isEmpty()) {
            sendLocalOnly(resumeId, eventName, payload, versionNo, taskId);
            return;
        }

        JsonNode payloadNode = objectMapper.valueToTree(payload);
        boolean localDelivered = false;

        for (String instanceId : instanceIds) {
            if (localInstanceId.equals(instanceId)) {
                sendLocalOnly(resumeId, eventName, payload, versionNo, taskId);
                localDelivered = true;
                continue;
            }
            try {
                sseDeliveryBus.publish(
                        new SseDeliveryEnvelope(
                                localInstanceId,
                                instanceId,
                                STREAM_TYPE_RESUME,
                                String.valueOf(resumeId),
                                eventName,
                                null,
                                payloadNode,
                                null));
            } catch (Exception ex) {
                log.warn(
                        "[RESUME_SSE] remote_publish_failed resumeId={} versionNo={} taskId={} targetInstanceId={}",
                        resumeId,
                        versionNo,
                        taskId,
                        instanceId,
                        ex);
            }
        }

        if (!localDelivered && sseEmitterRegistry.count(localStreamKey) > 0) {
            sendLocalOnly(resumeId, eventName, payload, versionNo, taskId);
        }
    }

    private void sendLocalOnly(
            Long resumeId, String eventName, Object payload, Integer versionNo, String taskId) {
        SseStreamKey streamKey = streamKey(resumeId);
        List<SseEmitter> emitters = sseEmitterRegistry.getEmitters(streamKey);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[RESUME_SSE] no_emitters resumeId={}", resumeId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                if (SseExceptionUtils.isClientDisconnected(ex)) {
                    log.debug(
                            "[RESUME_SSE] client_disconnected resumeId={} versionNo={} taskId={}",
                            resumeId,
                            versionNo,
                            taskId);
                } else {
                    log.warn(
                            "[RESUME_SSE] send_failed resumeId={} versionNo={} taskId={}",
                            resumeId,
                            versionNo,
                            taskId,
                            ex);
                }
                sseEmitterRegistry.completeWithError(streamKey, emitter, ex);
            }
        }
    }

    private SseRouteKey routeKey(Long resumeId) {
        return SseRouteKey.of(STREAM_TYPE_RESUME, resumeId);
    }

    private void refreshRouteTtl(SseStreamKey streamKey) {
        try {
            sseRouteRepository.upsertRoute(
                    SseRouteKey.of(streamKey.streamType(), streamKey.streamKey()),
                    sseInstanceIdProvider.getInstanceId(),
                    ROUTE_TTL);
        } catch (Exception ex) {
            log.debug(
                    "[RESUME_SSE] route_ttl_refresh_failed streamType={} streamKey={}",
                    streamKey.streamType(),
                    streamKey.streamKey(),
                    ex);
        }
    }

    private Long parseResumeId(String streamKey) {
        try {
            return Long.parseLong(streamKey);
        } catch (Exception ex) {
            log.warn("[RESUME_SSE] stream_key_invalid streamKey={}", streamKey, ex);
            return null;
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
