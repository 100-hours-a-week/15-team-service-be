package com.sipomeokjo.commitme.domain.worker.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record WorkerEventEnvelope(
        String eventId, String eventType, String idempotencyKey, JsonNode payload) {

    public WorkerEventType resolveType() {
        return WorkerEventType.from(eventType);
    }
}
