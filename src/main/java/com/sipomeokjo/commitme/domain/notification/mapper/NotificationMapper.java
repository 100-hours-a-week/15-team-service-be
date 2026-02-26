package com.sipomeokjo.commitme.domain.notification.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationListItem;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationMapper {
    private final ObjectMapper objectMapper;

    public NotificationListItem toItem(Notification notification) {
        JsonNode payloadNode = null;
        try {
            payloadNode = objectMapper.readTree(notification.getPayload());
        } catch (Exception ex) {
            log.warn(
                    "[NOTIFICATION] payload_parse_failed notificationId={}",
                    notification.getId(),
                    ex);
        }
        boolean read = notification.getReadAt() != null;
        return new NotificationListItem(
                notification.getId(),
                notification.getType(),
                payloadNode,
                read,
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
