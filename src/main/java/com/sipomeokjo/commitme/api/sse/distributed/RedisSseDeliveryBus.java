package com.sipomeokjo.commitme.api.sse.distributed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
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
    private final MeterRegistry meterRegistry;

    @Override
    public void publish(SseDeliveryEnvelope envelope) {
        long startedAtNanos = System.nanoTime();
        String channel =
                SseRedisChannelNames.deliveryChannelForInstance(envelope.targetInstanceId());
        try {
            String messageBody = objectMapper.writeValueAsString(envelope);
            recordPayloadBytes(envelope.streamType(), messageBody);
            Long subscriberCount = stringRedisTemplate.convertAndSend(channel, messageBody);
            meterRegistry.counter("sse_delivery_publish_total", "result", "success").increment();
            meterRegistry
                    .counter("sse_delivery_publish_subscribers_total")
                    .increment(subscriberCount.doubleValue());
            log.debug(
                    "[SSE_DELIVERY] published targetInstanceId={} streamType={} streamKey={} eventName={} subscriberCount={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    subscriberCount);
            recordPublishLatency(startedAtNanos, "success");
        } catch (JsonProcessingException ex) {
            meterRegistry
                    .counter("sse_delivery_publish_total", "result", "serialize_failed")
                    .increment();
            log.warn(
                    "[SSE_DELIVERY] serialize_failed targetInstanceId={} streamType={} streamKey={} eventName={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    ex);
            recordPublishLatency(startedAtNanos, "serialize_failed");
            throw new IllegalArgumentException("Failed to serialize SseDeliveryEnvelope", ex);
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter("sse_delivery_publish_total", "result", "publish_failed")
                    .increment();
            log.warn(
                    "[SSE_DELIVERY] publish_failed targetInstanceId={} streamType={} streamKey={} eventName={} channel={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    channel,
                    ex);
            recordPublishLatency(startedAtNanos, "publish_failed");
            throw ex;
        }
    }

    private void recordPublishLatency(long startedAtNanos, String result) {
        Timer.builder("sse_delivery_publish_duration")
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private void recordPayloadBytes(String streamType, String messageBody) {
        int payloadBytes = messageBody.getBytes(StandardCharsets.UTF_8).length;
        DistributionSummary.builder("sse_delivery_publish_payload_bytes")
                .tag("stream_type", streamType == null ? "unknown" : streamType)
                .register(meterRegistry)
                .record(payloadBytes);
    }
}
