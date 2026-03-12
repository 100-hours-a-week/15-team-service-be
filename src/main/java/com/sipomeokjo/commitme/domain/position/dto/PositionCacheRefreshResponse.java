package com.sipomeokjo.commitme.domain.position.dto;

public record PositionCacheRefreshResponse(
        long notifiedSubscriberCount, boolean localFallbackTriggered) {}
