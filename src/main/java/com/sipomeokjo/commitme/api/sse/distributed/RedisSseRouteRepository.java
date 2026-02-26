package com.sipomeokjo.commitme.api.sse.distributed;

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

    @Override
    public void upsertRoute(SseRouteKey routeKey, String instanceId, Duration ttl) {
        String normalizedInstanceId = normalizeInstanceId(instanceId);
        validateTtl(ttl);

        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        stringRedisTemplate.opsForSet().add(redisKey, normalizedInstanceId);
        stringRedisTemplate.expire(redisKey, ttl);
    }

    @Override
    public void removeRoute(SseRouteKey routeKey, String instanceId) {
        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        stringRedisTemplate.opsForSet().remove(redisKey, normalizeInstanceId(instanceId));
    }

    @Override
    public Set<String> findInstanceIds(SseRouteKey routeKey) {
        String redisKey = SseRedisChannelNames.routeKey(routeKey);
        Set<String> instanceIds = stringRedisTemplate.opsForSet().members(redisKey);
        return instanceIds == null ? Set.of() : Set.copyOf(instanceIds);
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
}
