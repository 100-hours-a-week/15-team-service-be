package com.sipomeokjo.commitme.api.sse.distributed;

import org.springframework.util.StringUtils;

public final class SseRedisChannelNames {
    private static final String ROUTE_KEY_PREFIX = "sse:route";
    private static final String DELIVERY_CHANNEL_PREFIX = "sse:delivery:instance";

    private SseRedisChannelNames() {}

    public static String routeKey(SseRouteKey routeKey) {
        return routeKey(routeKey.streamType(), routeKey.streamKey());
    }

    public static String routeKey(String streamType, String streamKey) {
        return ROUTE_KEY_PREFIX
                + ":"
                + requiredSegment(streamType, "streamType")
                + ":"
                + requiredSegment(streamKey, "streamKey");
    }

    public static String deliveryChannelForInstance(String instanceId) {
        return DELIVERY_CHANNEL_PREFIX + ":" + requiredSegment(instanceId, "instanceId");
    }

    private static String requiredSegment(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
