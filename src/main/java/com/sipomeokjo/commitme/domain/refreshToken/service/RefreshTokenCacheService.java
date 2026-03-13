package com.sipomeokjo.commitme.domain.refreshToken.service;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenCacheService {

    public enum AccessMode {
        ENABLED,
        BYPASS
    }

    private static final String KEY_PREFIX = "refresh:hash:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final AtomicReference<AccessMode> accessMode =
            new AtomicReference<>(AccessMode.ENABLED);

    public Optional<RefreshTokenCacheValue> get(String tokenHash) {
        if (isBypassEnabled()) {
            meterRegistry
                    .counter("refresh_token_cache_requests_total", "result", "bypass")
                    .increment();
            return Optional.empty();
        }

        long startedAtNanos = System.nanoTime();
        try {
            Object value = redisTemplate.opsForValue().get(key(tokenHash));
            if (value instanceof RefreshTokenCacheValue cached) {
                meterRegistry
                        .counter("refresh_token_cache_requests_total", "result", "hit")
                        .increment();
                recordDuration("refresh_token_cache_get_duration", startedAtNanos, "success");
                return Optional.of(cached);
            }
            if (value == null) {
                meterRegistry
                        .counter("refresh_token_cache_requests_total", "result", "miss")
                        .increment();
            } else {
                meterRegistry
                        .counter("refresh_token_cache_requests_total", "result", "type_mismatch")
                        .increment();
            }
            recordDuration("refresh_token_cache_get_duration", startedAtNanos, "success");
            return Optional.empty();
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter("refresh_token_cache_requests_total", "result", "error")
                    .increment();
            recordDuration("refresh_token_cache_get_duration", startedAtNanos, "failed");
            throw ex;
        }
    }

    public void cache(String tokenHash, Long userId, UserStatus status, Instant expiresAt) {
        if (isBypassEnabled()) {
            meterRegistry
                    .counter("refresh_token_cache_write_total", "result", "bypass")
                    .increment();
            return;
        }

        cacheDirect(tokenHash, userId, status, expiresAt);
    }

    public void cacheDirect(String tokenHash, Long userId, UserStatus status, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);

        if (ttl.isZero() || ttl.isNegative()) {
            meterRegistry
                    .counter("refresh_token_cache_write_total", "result", "skipped_expired")
                    .increment();
            return;
        }
        RefreshTokenCacheValue value = new RefreshTokenCacheValue(userId, status.name(), expiresAt);
        long startedAtNanos = System.nanoTime();
        try {
            redisTemplate.opsForValue().set(key(tokenHash), value, ttl);
            meterRegistry
                    .counter("refresh_token_cache_write_total", "result", "success")
                    .increment();
            recordDuration("refresh_token_cache_set_duration", startedAtNanos, "success");
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter("refresh_token_cache_write_total", "result", "failed")
                    .increment();
            recordDuration("refresh_token_cache_set_duration", startedAtNanos, "failed");
            throw ex;
        }
    }

    public void evict(String tokenHash) {
        if (isBypassEnabled()) {
            meterRegistry
                    .counter("refresh_token_cache_evict_total", "result", "bypass")
                    .increment();
            return;
        }

        evictDirect(tokenHash);
    }

    public boolean evictDirect(String tokenHash) {
        long startedAtNanos = System.nanoTime();
        try {
            Boolean deleted = redisTemplate.delete(key(tokenHash));
            if (Boolean.TRUE.equals(deleted)) {
                meterRegistry
                        .counter("refresh_token_cache_evict_total", "result", "success")
                        .increment();
            } else {
                meterRegistry
                        .counter("refresh_token_cache_evict_total", "result", "not_found")
                        .increment();
            }
            recordDuration("refresh_token_cache_evict_duration", startedAtNanos, "success");
            return Boolean.TRUE.equals(deleted);
        } catch (RuntimeException ex) {
            meterRegistry
                    .counter("refresh_token_cache_evict_total", "result", "failed")
                    .increment();
            recordDuration("refresh_token_cache_evict_duration", startedAtNanos, "failed");
            throw ex;
        }
    }

    public boolean existsDirect(String tokenHash) {
        Boolean exists = redisTemplate.hasKey(key(tokenHash));
        return Boolean.TRUE.equals(exists);
    }

    public AccessMode getAccessMode() {
        return accessMode.get();
    }

    public AccessMode setAccessMode(AccessMode mode) {
        AccessMode normalized = mode == null ? AccessMode.ENABLED : mode;
        accessMode.set(normalized);
        return normalized;
    }

    public boolean isBypassEnabled() {
        return getAccessMode() == AccessMode.BYPASS;
    }

    public void evictAll(Collection<String> tokenHashes) {
        if (tokenHashes == null || tokenHashes.isEmpty()) {
            return;
        }
        meterRegistry
                .counter("refresh_token_cache_evict_batch_total")
                .increment(tokenHashes.size());
        for (String tokenHash : tokenHashes) {
            evict(tokenHash);
        }
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }

    private void recordDuration(String metricName, long startedAtNanos, String result) {
        Timer.builder(metricName)
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenCacheValue {
        private Long userId;
        private String userStatus;
        private Instant expiresAt;
    }
}
