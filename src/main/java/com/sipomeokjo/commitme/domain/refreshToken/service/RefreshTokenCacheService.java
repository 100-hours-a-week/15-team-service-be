package com.sipomeokjo.commitme.domain.refreshToken.service;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenCacheService {

    private static final String KEY_PREFIX = "refresh:hash:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;

    public Optional<RefreshTokenCacheValue> get(String tokenHash) {
        Object value = redisTemplate.opsForValue().get(key(tokenHash));
        if (value instanceof RefreshTokenCacheValue cached) {
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    public void cache(String tokenHash, Long userId, UserStatus status, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);

        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        RefreshTokenCacheValue value = new RefreshTokenCacheValue(userId, status.name(), expiresAt);
        redisTemplate.opsForValue().set(key(tokenHash), value, ttl);
    }

    public void evict(String tokenHash) {
        redisTemplate.delete(key(tokenHash));
    }

    public void evictAll(Collection<String> tokenHashes) {
        for (String tokenHash : tokenHashes) {
            evict(tokenHash);
        }
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
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
