package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

public record LoadtestAuthResetResponse(
        String runId,
        int matchedAuthCount,
        int processedCount,
        int deactivatedUserCount,
        int revokedRefreshTokenCount) {}
