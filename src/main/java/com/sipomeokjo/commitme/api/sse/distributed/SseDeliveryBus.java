package com.sipomeokjo.commitme.api.sse.distributed;

public interface SseDeliveryBus {
    void publish(SseDeliveryEnvelope envelope);
}
