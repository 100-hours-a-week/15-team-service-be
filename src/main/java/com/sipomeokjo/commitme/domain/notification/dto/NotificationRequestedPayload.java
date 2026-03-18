package com.sipomeokjo.commitme.domain.notification.dto;

import java.time.Instant;

public record NotificationRequestedPayload(
        String notificationType,
        String sourceEventId,
        String messageId,
        Long chatroomId,
        String chatroomName,
        Long targetUserId,
        Long senderId,
        String senderName,
        Instant createdAt,
        String messagePreview) {}
