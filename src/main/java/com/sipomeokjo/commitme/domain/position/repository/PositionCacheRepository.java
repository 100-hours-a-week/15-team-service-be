package com.sipomeokjo.commitme.domain.position.repository;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Repository;

@Repository
public class PositionCacheRepository {
    private static final String CACHE_NAME = "positions";
    private static final String CACHE_KEY = "all";

    private final CacheManager cacheManager;

    public PositionCacheRepository(@Qualifier("localCacheManager") CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    public Optional<List<PositionResponse>> findAll() {
        Cache cache = getCache();
        Cache.ValueWrapper valueWrapper = cache.get(CACHE_KEY);
        if (valueWrapper == null) {
            return Optional.empty();
        }

        Object cachedValue = valueWrapper.get();
        if (!(cachedValue instanceof List<?>)) {
            return Optional.empty();
        }

        return Optional.of((List<PositionResponse>) cachedValue);
    }

    public void saveAll(List<PositionResponse> positions) {
        getCache().put(CACHE_KEY, List.copyOf(positions));
    }

    public boolean existsAll() {
        return getCache().get(CACHE_KEY) != null;
    }

    public boolean evictAll() {
        Cache cache = getCache();
        boolean existedBeforeEvict = cache.get(CACHE_KEY) != null;
        cache.evict(CACHE_KEY);
        return existedBeforeEvict;
    }

    private Cache getCache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("Cache not configured: " + CACHE_NAME);
        }
        return cache;
    }
}
