package com.sipomeokjo.commitme.api.sse;

import java.util.Objects;

public record SseStreamKey(String streamType, Long streamKey) {
    public SseStreamKey {
        Objects.requireNonNull(streamType, "streamType");
        Objects.requireNonNull(streamKey, "streamKey");
    }

    public static SseStreamKey of(String streamType, Long streamKey) {
        return new SseStreamKey(streamType, streamKey);
    }
}
