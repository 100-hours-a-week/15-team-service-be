package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record LoadtestAuthLoginResponse(
        Long userId,
        UserStatus status,
        String providerUserId,
        String providerUsername,
        boolean onboardingCompleted,
        String accessToken,
        String refreshToken,
        @JsonIgnore String cookieAccessToken,
        @JsonIgnore String cookieRefreshToken) {}
