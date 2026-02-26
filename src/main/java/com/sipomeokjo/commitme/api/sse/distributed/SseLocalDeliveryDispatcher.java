package com.sipomeokjo.commitme.api.sse.distributed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class SseLocalDeliveryDispatcher {
    private final Map<String, SseLocalDeliveryHandler> handlersByStreamType;

    public SseLocalDeliveryDispatcher(List<SseLocalDeliveryHandler> handlers) {
        Map<String, SseLocalDeliveryHandler> map = new LinkedHashMap<>();
        for (SseLocalDeliveryHandler handler : handlers) {
            String streamType = normalizeStreamType(handler.streamType());
            SseLocalDeliveryHandler previous = map.putIfAbsent(streamType, handler);
            if (previous != null) {
                log.warn("[SSE_LOCAL_DELIVERY] duplicate_handler streamType={}", streamType);
                throw new IllegalStateException(
                        "Duplicate SseLocalDeliveryHandler for streamType=" + streamType);
            }
        }
        this.handlersByStreamType = Map.copyOf(map);
    }

    public void dispatch(SseDeliveryEnvelope envelope) {
        SseLocalDeliveryHandler handler = handlersByStreamType.get(envelope.streamType());
        if (handler == null) {
            log.warn(
                    "[SSE_LOCAL_DELIVERY] handler_not_found streamType={} streamKey={} eventName={}",
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName());
            return;
        }
        handler.deliver(envelope);
    }

    private String normalizeStreamType(String streamType) {
        if (!StringUtils.hasText(streamType)) {
            throw new IllegalArgumentException("streamType must not be blank");
        }
        return streamType.trim();
    }
}
