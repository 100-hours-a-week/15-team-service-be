package com.sipomeokjo.commitme.domain.loadtest.dto;

public record LoadtestCleanupResponse(
        String runId,
        int targetUserCount,
        int deletedResumeCount,
        int deletedVersionCount,
        int deletedNotificationCount,
        int deletedRefreshTokenCount,
        int deletedPolicyAgreementCount,
        int deletedUserSettingCount,
        int deletedAuthCount,
        int deletedUserCount) {}
