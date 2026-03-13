package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;

public record LoadtestRefreshTokenCacheModeRequest(RefreshTokenCacheService.AccessMode mode) {}
