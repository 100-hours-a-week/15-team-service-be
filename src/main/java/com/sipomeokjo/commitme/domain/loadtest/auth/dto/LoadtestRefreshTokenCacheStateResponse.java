package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;

public record LoadtestRefreshTokenCacheStateResponse(
        String runId,
        RefreshTokenCacheService.AccessMode mode,
        int targetUserCount,
        int activeRefreshTokenCount,
        int cachedCacheEntryCount,
        int uncachedCacheEntryCount) {}
