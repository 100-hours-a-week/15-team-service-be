package com.sipomeokjo.commitme.domain.outbox.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record OutboxEventEnvelope(
        String eventId,
        String eventType,
        String producer,
        String idempotencyKey,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        JsonNode payload) {}
