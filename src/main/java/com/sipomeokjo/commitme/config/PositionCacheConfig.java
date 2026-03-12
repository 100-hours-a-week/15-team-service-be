package com.sipomeokjo.commitme.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PositionCacheConfig {

    @Bean("localCacheManager")
    public CacheManager localCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("positions");
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
