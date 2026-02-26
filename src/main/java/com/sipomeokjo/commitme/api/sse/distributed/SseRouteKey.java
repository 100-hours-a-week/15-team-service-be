package com.sipomeokjo.commitme.api.sse.distributed;

import org.springframework.util.StringUtils;

public record SseRouteKey(String streamType, String streamKey) {
    public SseRouteKey {
        streamType = requireText(streamType, "streamType");
        streamKey = requireText(streamKey, "streamKey");
    }

    public static SseRouteKey of(String streamType, Object streamKey) {
        return new SseRouteKey(streamType, String.valueOf(streamKey));
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
