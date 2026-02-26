package com.sipomeokjo.commitme.domain.notification.event;

import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;

public record NotificationCreateEvent(Long userId, NotificationType type, String payload) {}
