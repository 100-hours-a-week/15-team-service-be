package com.sipomeokjo.commitme.domain.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.sse.SseEmitterRegistry;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationSsePayload;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService {
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry sseEmitterRegistry;

    public SseEmitter subscribe(Long userId, String lastEventId) {
        SseEmitter emitter = sseEmitterRegistry.register(userId);

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            log.warn("[NOTIFICATION_SSE] connected_event_failed userId={}", userId, ex);
            sseEmitterRegistry.remove(userId, emitter);
            return emitter;
        }

        Long lastId = parseLastEventId(lastEventId);
        if (lastId != null) {
            replay(userId, lastId, emitter);
        }

        return emitter;
    }

    public void send(Notification notification) {
        if (notification == null || notification.getUser() == null) {
            return;
        }
        Long userId = notification.getUser().getId();
        List<SseEmitter> emitters = sseEmitterRegistry.getEmitters(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        NotificationSsePayload payload = toPayload(notification);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .id(String.valueOf(notification.getId()))
                                .name("notification")
                                .data(payload));
            } catch (IOException ex) {
                log.warn(
                        "[NOTIFICATION_SSE] send_failed userId={} notificationId={}",
                        userId,
                        notification.getId(),
                        ex);
                sseEmitterRegistry.remove(userId, emitter);
            }
        }
    }

    private void replay(Long userId, Long lastEventId, SseEmitter emitter) {
        List<Notification> notifications =
                notificationRepository.findByUser_IdAndIdGreaterThanOrderByIdAsc(
                        userId, lastEventId);
        if (notifications.isEmpty()) {
            return;
        }
        for (Notification notification : notifications) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .id(String.valueOf(notification.getId()))
                                .name("notification")
                                .data(toPayload(notification)));
            } catch (IOException ex) {
                log.warn(
                        "[NOTIFICATION_SSE] replay_failed userId={} notificationId={}",
                        userId,
                        notification.getId(),
                        ex);
                sseEmitterRegistry.remove(userId, emitter);
                return;
            }
        }
    }

    private NotificationSsePayload toPayload(Notification notification) {
        JsonNode payloadNode = null;
        try {
            payloadNode = objectMapper.readTree(notification.getPayload());
        } catch (Exception ex) {
            log.warn(
                    "[NOTIFICATION_SSE] payload_parse_failed notificationId={}",
                    notification.getId(),
                    ex);
        }
        return new NotificationSsePayload(
                notification.getId(),
                notification.getType(),
                payloadNode,
                notification.getCreatedAt());
    }

    private Long parseLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException ex) {
            log.warn("[NOTIFICATION_SSE] last_event_id_invalid value={}", lastEventId);
            return null;
        }
    }
}
