package com.sipomeokjo.commitme.api.sse.distributed;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = stringRedisSerializer.deserialize(message.getBody());

        if (!StringUtils.hasText(body)) {
            log.debug("[SSE_DELIVERY_SUBSCRIBER] empty_message_body");
            return;
        }

        SseDeliveryEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, SseDeliveryEnvelope.class);
        } catch (Exception ex) {
            log.warn("[SSE_DELIVERY_SUBSCRIBER] envelope_parse_failed", ex);
            return;
        }

        String localInstanceId = sseInstanceIdProvider.getInstanceId();
        if (!localInstanceId.equals(envelope.targetInstanceId())) {
            log.debug(
                    "[SSE_DELIVERY_SUBSCRIBER] target_mismatch localInstanceId={} targetInstanceId={} streamType={} streamKey={}",
                    localInstanceId,
                    envelope.targetInstanceId(),
                    envelope.streamType(),
                    envelope.streamKey());
            return;
        }

        try {
            sseLocalDeliveryDispatcher.dispatch(envelope);
        } catch (Exception ex) {
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
