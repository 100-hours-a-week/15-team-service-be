package com.sipomeokjo.commitme.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.event.NotificationCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, NotificationType type, Object payload) {
        if (userId == null || type == null || payload == null) {
            return;
        }

        String payloadJson;
        try {
            payloadJson =
                    (payload instanceof String payloadString)
                            ? payloadString
                            : objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("[NOTIFICATION] payload_serialize_failed userId={} type={}", userId, type, ex);
            return;
        }

        eventPublisher.publishEvent(new NotificationCreateEvent(userId, type, payloadJson));
    }
}
