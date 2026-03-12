package com.sipomeokjo.commitme.domain.position.service;

import io.micrometer.core.instrument.MeterRegistry;
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
public class PositionCacheRefreshSubscriber implements MessageListener {
    private final PositionCacheWarmupService positionCacheWarmupService;
    private final MeterRegistry meterRegistry;
    private final StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = stringRedisSerializer.deserialize(message.getBody());
        if (!StringUtils.hasText(body)) {
            meterRegistry
                    .counter(
                            "positions_cache_refresh_subscriber_total",
                            "result",
                            "empty_message_body")
                    .increment();
            log.debug("[PositionCacheRefresh] empty_message_body");
            return;
        }

        String trigger = body.trim();
        meterRegistry
                .counter("positions_cache_refresh_subscriber_total", "result", "received")
                .increment();
        log.info("[PositionCacheRefresh] received trigger={}", trigger);
        positionCacheWarmupService.refreshAsync("redis_broadcast:" + trigger);
    }
}
