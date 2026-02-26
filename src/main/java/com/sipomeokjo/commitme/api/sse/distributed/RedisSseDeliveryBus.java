package com.sipomeokjo.commitme.api.sse.distributed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSseDeliveryBus implements SseDeliveryBus {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(SseDeliveryEnvelope envelope) {
        String channel =
                SseRedisChannelNames.deliveryChannelForInstance(envelope.targetInstanceId());
        try {
            String messageBody = objectMapper.writeValueAsString(envelope);
            Long subscriberCount = stringRedisTemplate.convertAndSend(channel, messageBody);
            log.debug(
                    "[SSE_DELIVERY] published targetInstanceId={} streamType={} streamKey={} eventName={} subscriberCount={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    subscriberCount);
        } catch (JsonProcessingException ex) {
            log.warn(
                    "[SSE_DELIVERY] serialize_failed targetInstanceId={} streamType={} streamKey={} eventName={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    ex);
            throw new IllegalArgumentException("Failed to serialize SseDeliveryEnvelope", ex);
        } catch (RuntimeException ex) {
            log.warn(
                    "[SSE_DELIVERY] publish_failed targetInstanceId={} streamType={} streamKey={} eventName={} channel={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    channel,
                    ex);
            throw ex;
        }
    }
}
