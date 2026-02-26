package com.sipomeokjo.commitme.domain.interview.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class InterviewSseEmitterManager {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(Long interviewId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(
                () -> {
                    log.info("SSE completed for interview: {}", interviewId);
                    emitters.remove(interviewId);
                });

        emitter.onTimeout(
                () -> {
                    log.info("SSE timeout for interview: {}", interviewId);
                    emitters.remove(interviewId);
                });

        emitter.onError(
                e -> {
                    log.error("SSE error for interview: {}", interviewId, e);
                    emitters.remove(interviewId);
                });

        emitters.put(interviewId, emitter);
        return emitter;
    }

    public void sendQuestion(Long interviewId, Object data) {
        send(interviewId, "question", data);
    }

    public void sendFeedback(Long interviewId, Object data) {
        send(interviewId, "feedback", data);
    }

    public void sendEnd(Long interviewId) {
        send(interviewId, "end", Map.of("message", "면접이 종료되었습니다."));
        remove(interviewId);
    }

    public void send(Long interviewId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(interviewId);
        if (emitter == null) {
            log.warn("No SSE emitter found for interview: {}", interviewId);
            return;
        }

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.error("Failed to send SSE event for interview: {}", interviewId, e);
            emitters.remove(interviewId);
        }
    }

    public void remove(Long interviewId) {
        SseEmitter emitter = emitters.remove(interviewId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public boolean exists(Long interviewId) {
        return emitters.containsKey(interviewId);
    }
}
