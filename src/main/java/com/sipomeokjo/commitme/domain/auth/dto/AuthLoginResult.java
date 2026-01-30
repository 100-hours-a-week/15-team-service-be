package com.sipomeokjo.commitme.domain.auth.dto;

public record AuthLoginResult(
        String accessToken, String refreshToken, boolean onboardingCompleted) {}
