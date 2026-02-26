package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

public record LoadtestAuthLogoutResponse(
        Long userId, String providerUserId, int revokedRefreshTokenCount) {}
