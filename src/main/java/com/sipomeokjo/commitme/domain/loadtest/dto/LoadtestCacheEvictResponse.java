package com.sipomeokjo.commitme.domain.loadtest.dto;

public record LoadtestCacheEvictResponse(
        String cacheName, String key, boolean existedBeforeEvict) {}
