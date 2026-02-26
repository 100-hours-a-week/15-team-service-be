package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record LoadtestAuthBulkCreateItemResponse(
        Long userId,
        UserStatus status,
        String providerUserId,
        String providerUsername,
        String accessToken,
        String refreshToken) {}
