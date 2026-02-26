package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record LoadtestAuthSignupResponse(
        Long userId, UserStatus status, String providerUserId, String providerUsername) {}
