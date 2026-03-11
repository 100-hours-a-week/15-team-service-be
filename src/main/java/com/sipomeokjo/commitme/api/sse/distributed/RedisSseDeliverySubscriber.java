package com.sipomeokjo.commitme.api.sse.distributed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSseDeliverySubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SseInstanceIdProvider sseInstanceIdProvider;
    private final SseLocalDeliveryDispatcher sseLocalDeliveryDispatcher;
    private final MeterRegistry meterRegistry;
    private final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = stringRedisSerializer.deserialize(message.getBody());
        if (body != null) {
            DistributionSummary.builder("sse_delivery_subscriber_payload_bytes")
                    .register(meterRegistry)
                    .record(body.getBytes(StandardCharsets.UTF_8).length);
        }

        if (!StringUtils.hasText(body)) {
            meterRegistry
                    .counter(
                            "sse_delivery_subscriber_messages_total",
                            "result",
                            "empty_message_body")
                    .increment();
            log.debug("[SSE_DELIVERY_SUBSCRIBER] empty_message_body");
            return;
        }

        SseDeliveryEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, SseDeliveryEnvelope.class);
        } catch (Exception ex) {
            meterRegistry
                    .counter(
                            "sse_delivery_subscriber_messages_total",
                            "result",
                            "envelope_parse_failed")
                    .increment();
            log.warn("[SSE_DELIVERY_SUBSCRIBER] envelope_parse_failed", ex);
            return;
        }

        String localInstanceId = sseInstanceIdProvider.getInstanceId();
        if (!localInstanceId.equals(envelope.targetInstanceId())) {
            meterRegistry
                    .counter("sse_delivery_subscriber_messages_total", "result", "target_mismatch")
                    .increment();
            log.debug(
                    "[SSE_DELIVERY_SUBSCRIBER] target_mismatch localInstanceId={} targetInstanceId={} streamType={} streamKey={}",
                    localInstanceId,
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey());
            return;
        }

        long dispatchStartedAtNanos = System.nanoTime();
        try {
            sseLocalDeliveryDispatcher.dispatch(envelope);
            meterRegistry
                    .counter("sse_delivery_subscriber_messages_total", "result", "dispatch_success")
                    .increment();
            Timer.builder("sse_delivery_subscriber_dispatch_duration")
                    .tag("result", "success")
                    .register(meterRegistry)
                    .record(
                            System.nanoTime() - dispatchStartedAtNanos,
                            java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (Exception ex) {
            meterRegistry
                    .counter("sse_delivery_subscriber_messages_total", "result", "dispatch_failed")
                    .increment();
            Timer.builder("sse_delivery_subscriber_dispatch_duration")
                    .tag("result", "failed")
                    .register(meterRegistry)
                    .record(
                            System.nanoTime() - dispatchStartedAtNanos,
                            java.util.concurrent.TimeUnit.NANOSECONDS);
            log.warn(
                    "[SSE_DELIVERY_SUBSCRIBER] dispatch_failed targetInstanceId={} streamType={} streamKey={} eventName={}",
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey(),
                    envelope.eventName(),
                    ex);
        }
    }
}
