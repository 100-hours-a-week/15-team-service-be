package com.sipomeokjo.commitme.api.sse;

import java.io.IOException;
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

    private final SseEmitterRegistry sseEmitterRegistry;

    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeat() {
        sseEmitterRegistry.forEachEmitter(
                (key, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event().name(HEARTBEAT_EVENT).data(HEARTBEAT_DATA));
                    } catch (IOException ex) {
                        log.debug("[SSE_HEARTBEAT] send_failed key={}", key, ex);
                        sseEmitterRegistry.remove(key, emitter);
                    }
                });
    }
}
