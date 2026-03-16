package com.sipomeokjo.commitme.domain.notification.dto;

import java.time.Instant;

public record NotificationRequestedPayload(
        String notificationType,
        String sourceEventId,
        String messageId,
        Long chatroomId,
        Long targetUserId,
        Long senderId,
        Instant createdAt,
        String messagePreview) {}
