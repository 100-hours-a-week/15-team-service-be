package com.sipomeokjo.commitme.domain.notification.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import java.time.Instant;

public record NotificationListItem(
        Long id,
        NotificationType type,
        JsonNode payload,
        boolean read,
        Instant readAt,
        Instant createdAt) {}
