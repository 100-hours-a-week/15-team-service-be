package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.stereotype.Component;

@Component
public class PositionLocalCache {
    public static final String CACHE_NAME = "positions";
    public static final String CACHE_KEY = "all";

    private final ConcurrentMapCache cache = new ConcurrentMapCache(CACHE_NAME, false);

    @SuppressWarnings("unchecked")
    public List<PositionResponse> getAll() {
        Cache.ValueWrapper valueWrapper = cache.get(CACHE_KEY);
        if (valueWrapper == null) {
            return null;
        }

        Object cachedValue = valueWrapper.get();
        if (cachedValue instanceof List<?>) {
            return (List<PositionResponse>) cachedValue;
        }
        return null;
    }

    public void putAll(List<PositionResponse> positions) {
        cache.put(CACHE_KEY, List.copyOf(positions));
    }

    public boolean contains(String key) {
        return cache.get(key) != null;
    }

    public boolean containsAll() {
        return contains(CACHE_KEY);
    }

    public boolean evict(String key) {
        boolean existedBeforeEvict = contains(key);
        cache.evict(key);
        return existedBeforeEvict;
    }

    public boolean evictAll() {
        return evict(CACHE_KEY);
    }
}
