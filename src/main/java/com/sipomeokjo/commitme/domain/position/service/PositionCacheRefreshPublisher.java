package com.sipomeokjo.commitme.domain.position.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class PositionCacheRefreshPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    public long publishRefresh(String trigger) {
        String normalizedTrigger = normalizeTrigger(trigger);
        long startedAtNanos = System.nanoTime();

        try {
            long notifiedSubscriberCount =
                    stringRedisTemplate.convertAndSend(
                            PositionCacheChannels.REFRESH_CHANNEL, normalizedTrigger);

            meterRegistry
                    .counter("positions_cache_refresh_publish_total", "result", "success")
                    .increment();
            recordPublishDuration(startedAtNanos, "success");

            log.info(
                    "[PositionCacheRefresh] published trigger={} subscriberCount={}",
                    normalizedTrigger,
                    notifiedSubscriberCount);
            return notifiedSubscriberCount;
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter("positions_cache_refresh_publish_total", "result", "failed")
                    .increment();
            recordPublishDuration(startedAtNanos, "failed");
            log.warn("[PositionCacheRefresh] publish_failed trigger={}", normalizedTrigger, ex);
            throw ex;
        }
    }

    private void recordPublishDuration(long startedAtNanos, String result) {
        Timer.builder("positions_cache_refresh_publish_duration")
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private String normalizeTrigger(String trigger) {
        if (!StringUtils.hasText(trigger)) {
            return "manual_refresh";
        }
        return trigger.trim();
    }
}
