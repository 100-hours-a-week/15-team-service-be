package com.sipomeokjo.commitme.api.sse.distributed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import org.springframework.util.StringUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SseDeliveryEnvelope(
        String sourceInstanceId,
        String targetInstanceId,
        String streamType,
        String streamKey,
        String eventName,
        String eventId,
        JsonNode data,
        Instant createdAt) {
    public SseDeliveryEnvelope {
        sourceInstanceId = requireText(sourceInstanceId, "sourceInstanceId");
        targetInstanceId = requireText(targetInstanceId, "targetInstanceId");
        streamType = requireText(streamType, "streamType");
        streamKey = requireText(streamKey, "streamKey");
        eventName = requireText(eventName, "eventName");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public SseRouteKey routeKey() {
        return new SseRouteKey(streamType, streamKey);
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
