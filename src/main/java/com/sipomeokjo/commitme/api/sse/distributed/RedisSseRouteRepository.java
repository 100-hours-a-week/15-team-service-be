package com.sipomeokjo.commitme.api.sse.distributed;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class RedisSseRouteRepository implements SseRouteRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public void upsertRoute(SseRouteKey routeKey, String instanceId, Duration ttl) {
        String normalizedInstanceId = normalizeInstanceId(instanceId);
        validateTtl(ttl);

        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        long startedAtNanos = System.nanoTime();
        try {
            stringRedisTemplate.opsForSet().add(redisKey, normalizedInstanceId);
            stringRedisTemplate.expire(redisKey, ttl);
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "upsert",
                            "result",
                            "success")
                    .increment();
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "upsert",
                            "result",
                            "failed")
                    .increment();
            recordOperationDuration("upsert", "failed", startedAtNanos);
            throw ex;
        }
        recordOperationDuration("upsert", "success", startedAtNanos);
    }

    @Override
    public void removeRoute(SseRouteKey routeKey, String instanceId) {
        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        long startedAtNanos = System.nanoTime();
        try {
            stringRedisTemplate.opsForSet().remove(redisKey, normalizeInstanceId(instanceId));
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "remove",
                            "result",
                            "success")
                    .increment();
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "remove",
                            "result",
                            "failed")
                    .increment();
            recordOperationDuration("remove", "failed", startedAtNanos);
            throw ex;
        }
        recordOperationDuration("remove", "success", startedAtNanos);
    }

    @Override
    public Set<String> findInstanceIds(SseRouteKey routeKey) {
        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        long startedAtNanos = System.nanoTime();
        try {
            Set<String> instanceIds = stringRedisTemplate.opsForSet().members(redisKey);
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "find",
                            "result",
                            "success")
                    .increment();
            meterRegistry
                    .counter("sse_route_repository_instances_found_total")
                    .increment(instanceIds == null ? 0D : instanceIds.size());
            recordOperationDuration("find", "success", startedAtNanos);
            return instanceIds == null ? Set.of() : Set.copyOf(instanceIds);
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter(
                            "sse_route_repository_operations_total",
                            "operation",
                            "find",
                            "result",
                            "failed")
                    .increment();
            recordOperationDuration("find", "failed", startedAtNanos);
            throw ex;
        }
    }

    private String normalizeInstanceId(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        return instanceId.trim();
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
    }

    private void recordOperationDuration(String operation, String result, long startedAtNanos) {
        Timer.builder("sse_route_repository_operation_duration")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
