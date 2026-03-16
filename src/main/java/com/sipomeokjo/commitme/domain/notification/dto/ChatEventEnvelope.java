package com.sipomeokjo.commitme.domain.notification.dto;

import java.time.Instant;

public record ChatEventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        String producer,
        String idempotencyKey,
        T payload) {}
