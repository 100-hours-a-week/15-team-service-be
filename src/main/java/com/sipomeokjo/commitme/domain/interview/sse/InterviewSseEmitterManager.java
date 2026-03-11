package com.sipomeokjo.commitme.domain.interview.sse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryBus;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryEnvelope;
import com.sipomeokjo.commitme.api.sse.distributed.SseInstanceIdProvider;
import com.sipomeokjo.commitme.api.sse.distributed.SseLocalDeliveryHandler;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewSseEmitterManager implements SseLocalDeliveryHandler {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final String STREAM_TYPE = "interview";
    private static final String METRIC_STREAM_TYPE = "interview";
    private static final Duration ROUTE_TTL = Duration.ofMinutes(35); // SSE_TIMEOUT보다 조금 길게

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final SseRouteRepository sseRouteRepository;
    private final SseDeliveryBus sseDeliveryBus;
    private final SseInstanceIdProvider sseInstanceIdProvider;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public SseEmitter create(Long interviewId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        meterRegistry.counter("interview_sse_subscribe_total").increment();
        meterRegistry.counter("sse_subscribe_total", "stream_type", METRIC_STREAM_TYPE).increment();

        emitter.onCompletion(
                () -> {
                    log.info("SSE completed for interview: {}", interviewId);
                    meterRegistry
                            .counter("interview_sse_disconnect_total", "reason", "completion")
                            .increment();
                    removeIfCurrent(interviewId, emitter);
                });

        emitter.onTimeout(
                () -> {
                    log.info("SSE timeout for interview: {}", interviewId);
                    meterRegistry
                            .counter("interview_sse_disconnect_total", "reason", "timeout")
                            .increment();
                    removeIfCurrent(interviewId, emitter);
                });

        emitter.onError(
                e -> {
                    log.error("SSE error for interview: {}", interviewId, e);
                    meterRegistry
                            .counter("interview_sse_disconnect_total", "reason", "error")
                            .increment();
                    removeIfCurrent(interviewId, emitter);
                });

        SseEmitter previous = emitters.put(interviewId, emitter);
        if (previous != null) {
            previous.complete();
        }

        // Redis에 라우팅 정보 저장
        refreshRouteTtl(interviewId);

        return emitter;
    }

    public void sendQuestion(Long interviewId, Object data) {
        sendDistributed(interviewId, "question", data);
    }

    public void sendFeedback(Long interviewId, Object data) {
        sendDistributed(interviewId, "feedback", data);
    }

    public void sendEnd(Long interviewId) {
        sendDistributed(interviewId, "end", Map.of("message", "면접이 종료되었습니다."));
        removeDistributed(interviewId);
    }

    public void sendAllQuestionsComplete(Long interviewId) {
        send(interviewId, "allQuestionsComplete", Map.of("message", "모든 질문이 완료되었습니다."));
    }

    public void send(Long interviewId, String eventName, Object data) {
        sendDistributed(interviewId, eventName, data);
    }

    public void remove(Long interviewId) {
        removeDistributed(interviewId);
    }

    public boolean exists(Long interviewId) {
        return emitters.containsKey(interviewId);
    }

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }

    @Override
    public void deliver(SseDeliveryEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        log.debug(
                "[INTERVIEW_SSE] remote_delivery_received streamKey={} eventName={}",
                envelope.streamKey(),
                envelope.eventName());

        Long interviewId = parseInterviewId(envelope.streamKey());
        if (interviewId == null) {
            return;
        }

        // 종료 이벤트 처리
        if ("__remove__".equals(envelope.eventName())) {
            removeLocal(interviewId);
            return;
        }

        Object payload;
        try {
            if (envelope.data() == null) {
                return;
            }
            payload = objectMapper.treeToValue(envelope.data(), Object.class);
        } catch (Exception ex) {
            log.warn(
                    "[INTERVIEW_SSE] remote_payload_parse_failed streamKey={} eventName={}",
                    envelope.streamKey(),
                    envelope.eventName(),
                    ex);
            return;
        }

        sendLocal(interviewId, envelope.eventName(), payload);
    }

    private void sendDistributed(Long interviewId, String eventName, Object data) {
        SseRouteKey routeKey = routeKey(interviewId);
        String localInstanceId = sseInstanceIdProvider.getInstanceId();

        Set<String> instanceIds;
        try {
            instanceIds = sseRouteRepository.findInstanceIds(routeKey);
            meterRegistry
                    .counter("interview_sse_route_lookup_total", "result", "success")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_route_lookup_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("interview_sse_route_lookup_total", "result", "failed")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_route_lookup_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "failed")
                    .increment();
            log.warn("[INTERVIEW_SSE] route_lookup_failed interviewId={}", interviewId, ex);
            sendLocal(interviewId, eventName, data);
            return;
        }

        if (instanceIds.isEmpty()) {
            meterRegistry
                    .counter("interview_sse_route_lookup_total", "result", "empty")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_route_lookup_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "empty")
                    .increment();
            log.debug(
                    "[INTERVIEW_SSE] route_instances_empty interviewId={} eventName={}",
                    interviewId,
                    eventName);
            sendLocal(interviewId, eventName, data);
            return;
        }

        JsonNode payloadNode = objectMapper.valueToTree(data);
        boolean localDelivered = false;

        for (String instanceId : instanceIds) {
            if (localInstanceId.equals(instanceId)) {
                sendLocal(interviewId, eventName, data);
                localDelivered = true;
                continue;
            }

            try {
                sseDeliveryBus.publish(
                        new SseDeliveryEnvelope(
                                localInstanceId,
                                instanceId,
                                STREAM_TYPE,
                                String.valueOf(interviewId),
                                eventName,
                                null,
                                payloadNode,
                                null));
                meterRegistry
                        .counter("interview_sse_remote_publish_total", "result", "success")
                        .increment();
            } catch (Exception ex) {
                meterRegistry
                        .counter("interview_sse_remote_publish_total", "result", "failed")
                        .increment();
                log.warn(
                        "[INTERVIEW_SSE] remote_publish_failed interviewId={} targetInstanceId={}",
                        interviewId,
                        instanceId,
                        ex);
            }
        }

        // 로컬 emitter가 있는데 라우팅 정보에 없는 경우 대비
        if (!localDelivered && emitters.containsKey(interviewId)) {
            sendLocal(interviewId, eventName, data);
        }
    }

    private void sendLocal(Long interviewId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(interviewId);
        if (emitter == null) {
            log.warn("No SSE emitter found for interview: {}", interviewId);
            meterRegistry
                    .counter("interview_sse_local_delivery_total", "result", "no_emitter")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_local_delivery_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "no_emitter")
                    .increment();
            return;
        }

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            meterRegistry
                    .counter("interview_sse_local_delivery_total", "result", "success")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_local_delivery_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "success")
                    .increment();
        } catch (IOException e) {
            meterRegistry
                    .counter("interview_sse_local_delivery_total", "result", "failed")
                    .increment();
            meterRegistry
                    .counter(
                            "sse_local_delivery_total",
                            "stream_type",
                            METRIC_STREAM_TYPE,
                            "result",
                            "failed")
                    .increment();
            log.error("Failed to send SSE event for interview: {}", interviewId, e);
            removeIfCurrent(interviewId, emitter);
        }
    }

    private void removeDistributed(Long interviewId) {
        SseRouteKey routeKey = routeKey(interviewId);
        String localInstanceId = sseInstanceIdProvider.getInstanceId();

        Set<String> instanceIds;
        try {
            instanceIds = sseRouteRepository.findInstanceIds(routeKey);
            meterRegistry
                    .counter("interview_sse_route_lookup_total", "result", "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("interview_sse_route_lookup_total", "result", "failed")
                    .increment();
            log.warn(
                    "[INTERVIEW_SSE] route_lookup_failed on remove interviewId={}",
                    interviewId,
                    ex);
            removeLocal(interviewId);
            return;
        }

        for (String instanceId : instanceIds) {
            if (localInstanceId.equals(instanceId)) {
                removeLocal(interviewId);
                continue;
            }

            try {
                sseDeliveryBus.publish(
                        new SseDeliveryEnvelope(
                                localInstanceId,
                                instanceId,
                                STREAM_TYPE,
                                String.valueOf(interviewId),
                                "__remove__",
                                null,
                                null,
                                null));
                meterRegistry
                        .counter("interview_sse_remote_publish_total", "result", "success")
                        .increment();
            } catch (Exception ex) {
                meterRegistry
                        .counter("interview_sse_remote_publish_total", "result", "failed")
                        .increment();
                log.warn(
                        "[INTERVIEW_SSE] remote_remove_publish_failed interviewId={} targetInstanceId={}",
                        interviewId,
                        instanceId,
                        ex);
            }
        }

        // 라우팅 정보 삭제
        try {
            sseRouteRepository.removeRoute(routeKey, localInstanceId);
            meterRegistry
                    .counter("interview_sse_route_remove_total", "result", "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("interview_sse_route_remove_total", "result", "failed")
                    .increment();
            log.warn("[INTERVIEW_SSE] route_remove_failed interviewId={}", interviewId, ex);
        }
    }

    private void removeLocal(Long interviewId) {
        SseEmitter emitter = emitters.remove(interviewId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    private void removeIfCurrent(Long interviewId, SseEmitter emitter) {
        boolean removed = emitters.remove(interviewId, emitter);
        if (removed) {
            // 라우팅 정보도 삭제
            try {
                sseRouteRepository.removeRoute(
                        routeKey(interviewId), sseInstanceIdProvider.getInstanceId());
                meterRegistry
                        .counter("interview_sse_route_remove_total", "result", "success")
                        .increment();
            } catch (Exception ex) {
                meterRegistry
                        .counter("interview_sse_route_remove_total", "result", "failed")
                        .increment();
                log.warn(
                        "[INTERVIEW_SSE] route_remove_failed on cleanup interviewId={}",
                        interviewId,
                        ex);
            }
        }
    }

    private void refreshRouteTtl(Long interviewId) {
        try {
            sseRouteRepository.upsertRoute(
                    routeKey(interviewId), sseInstanceIdProvider.getInstanceId(), ROUTE_TTL);
            meterRegistry
                    .counter("interview_sse_route_ttl_refresh_total", "result", "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("interview_sse_route_ttl_refresh_total", "result", "failed")
                    .increment();
            log.warn("[INTERVIEW_SSE] route_ttl_refresh_failed interviewId={}", interviewId, ex);
        }
    }

    private SseRouteKey routeKey(Long interviewId) {
        return SseRouteKey.of(STREAM_TYPE, interviewId);
    }

    private Long parseInterviewId(String streamKey) {
        try {
            return Long.parseLong(streamKey);
        } catch (Exception ex) {
            log.warn("[INTERVIEW_SSE] stream_key_invalid streamKey={}", streamKey, ex);
            return null;
        }
    }
}
