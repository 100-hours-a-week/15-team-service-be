package com.sipomeokjo.commitme.api.sse.distributed;

public interface SseLocalDeliveryHandler {
    String streamType();

    void deliver(SseDeliveryEnvelope envelope);
}
