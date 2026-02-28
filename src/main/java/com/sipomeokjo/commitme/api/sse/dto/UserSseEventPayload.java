package com.sipomeokjo.commitme.api.sse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record UserSseEventPayload(UserSseEventType eventType, Instant occurredAt, JsonNode data) {}
