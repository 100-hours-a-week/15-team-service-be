package com.sipomeokjo.commitme.domain.position.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCacheWarmupService {
    private final PositionQueryService positionQueryService;
    private final MeterRegistry meterRegistry;

    @Async
    public void warmUpAsync(String trigger) {
        executeWarmup(trigger, false);
    }

    @Async
    public void refreshAsync(String trigger) {
        executeWarmup(trigger, true);
    }

    private void executeWarmup(String trigger, boolean evictFirst) {
        String normalizedTrigger = normalizeTrigger(trigger);
        long startedAtNanos = System.nanoTime();

        log.info(
                "[PositionCacheWarmup] started trigger={} evictFirst={}",
                normalizedTrigger,
                evictFirst);

        try {
            boolean existedBeforeEvict = false;
            if (evictFirst) {
                existedBeforeEvict = positionQueryService.evictCachedPositions();
            }

            int loadedCount = positionQueryService.warmUpCache();
            meterRegistry
                    .counter(
                            "positions_cache_warmup_total",
                            "result",
                            "success",
                            "trigger",
                            normalizedTrigger)
                    .increment();
            recordWarmupDuration(startedAtNanos, normalizedTrigger, "success");

            log.info(
                    "[PositionCacheWarmup] completed trigger={} evictFirst={} existedBeforeEvict={} loadedCount={}",
                    normalizedTrigger,
                    evictFirst,
                    existedBeforeEvict,
                    loadedCount);
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter(
                            "positions_cache_warmup_total",
                            "result",
                            "failed",
                            "trigger",
                            normalizedTrigger)
                    .increment();
            recordWarmupDuration(startedAtNanos, normalizedTrigger, "failed");
            log.warn(
                    "[PositionCacheWarmup] failed trigger={} evictFirst={}",
                    normalizedTrigger,
                    evictFirst,
                    ex);
        }
    }

    private void recordWarmupDuration(long startedAtNanos, String trigger, String result) {
        Timer.builder("positions_cache_warmup_duration")
                .tag("trigger", trigger)
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    private String normalizeTrigger(String trigger) {
        if (!StringUtils.hasText(trigger)) {
            return "unknown";
        }
        return trigger.trim();
    }
}
