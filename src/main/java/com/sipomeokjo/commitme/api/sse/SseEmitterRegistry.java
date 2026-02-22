package com.sipomeokjo.commitme.api.sse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry {
    private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L;
    private final Map<Long, List<SseEmitter>> emittersByKey = new ConcurrentHashMap<>();

    public SseEmitter register(Long key) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emittersByKey.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(ex -> remove(key, emitter));

        return emitter;
    }

    public List<SseEmitter> getEmitters(Long key) {
        return emittersByKey.get(key);
    }

    public void forEachEmitter(BiConsumer<Long, SseEmitter> consumer) {
        emittersByKey.forEach(
                (key, emitters) -> {
                    for (SseEmitter emitter : emitters) {
                        consumer.accept(key, emitter);
                    }
                });
    }

    public void remove(Long key, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByKey.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByKey.remove(key);
        }
    }

    public void completeWithError(Long key, SseEmitter emitter, Throwable throwable) {
        try {
            emitter.completeWithError(throwable);
        } catch (Exception ignored) {
            // Emitter가 제거 혹은 connection 끊긴 상태이므로 추가적인 예외 처리 필요하지 않음
        } finally {
            remove(key, emitter);
        }
    }

    public int count(Long key) {
        List<SseEmitter> emitters = emittersByKey.get(key);
        return emitters == null ? 0 : emitters.size();
    }
}
