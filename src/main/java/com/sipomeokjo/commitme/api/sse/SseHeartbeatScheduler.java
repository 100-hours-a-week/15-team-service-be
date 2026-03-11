package com.sipomeokjo.commitme.api.sse;

import com.sipomeokjo.commitme.api.sse.distributed.SseInstanceIdProvider;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {
    private static final String HEARTBEAT_EVENT = "heartbeat";
    private static final String HEARTBEAT_DATA = "ok";
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    private static final Duration ROUTE_TTL = Duration.ofMinutes(2);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final SseRouteRepository sseRouteRepository;
    private final SseInstanceIdProvider sseInstanceIdProvider;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        meterRegistry.counter("sse_heartbeat_ticks_total").increment();
        Set<SseStreamKey> refreshedKeys = new HashSet<>();
        String instanceId = sseInstanceIdProvider.getInstanceId();
        sseEmitterRegistry.forEachEmitter(
                (key, emitter) -> {
                    if (refreshedKeys.add(key)) {
                        refreshRouteTtl(key, instanceId);
                    }
                    try {
                        emitter.send(SseEmitter.event().name(HEARTBEAT_EVENT).data(HEARTBEAT_DATA));
                        meterRegistry
                                .counter("sse_heartbeat_send_total", "result", "success")
                                .increment();
                    } catch (Exception ex) {
                        if (SseExceptionUtils.isClientDisconnected(ex)) {
                            meterRegistry
                                    .counter(
                                            "sse_heartbeat_send_total",
                                            "result",
                                            "client_disconnected")
                                    .increment();
                            log.debug(
                                    "[SSE_HEARTBEAT] client_disconnected streamType={} streamKey={}",
                                    key.streamType(),
                                    key.streamKey());
                        } else {
                            meterRegistry
                                    .counter("sse_heartbeat_send_total", "result", "failed")
                                    .increment();
                            log.warn(
                                    "[SSE_HEARTBEAT] send_failed streamType={} streamKey={}",
                                    key.streamType(),
                                    key.streamKey(),
                                    ex);
                        }
                        sseEmitterRegistry.completeWithError(key, emitter, ex);
                    }
                });
    }

    private void refreshRouteTtl(SseStreamKey key, String instanceId) {
        try {
            sseRouteRepository.upsertRoute(
                    SseRouteKey.of(key.streamType(), key.streamKey()), instanceId, ROUTE_TTL);
            meterRegistry
                    .counter("sse_heartbeat_route_ttl_refresh_total", "result", "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("sse_heartbeat_route_ttl_refresh_total", "result", "failed")
                    .increment();
            log.debug(
                    "[SSE_HEARTBEAT] route_ttl_refresh_failed streamType={} streamKey={} instanceId={}",
                    key.streamType(),
                    key.streamKey(),
                    instanceId,
                    ex);
        }
    }
}
