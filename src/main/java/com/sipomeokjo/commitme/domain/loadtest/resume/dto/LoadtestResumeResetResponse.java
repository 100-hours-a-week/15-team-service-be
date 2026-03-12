package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeResetResponse(
        String runId,
        int targetUserCount,
        int deletedResumeCount,
        int deletedVersionCount,
        int deletedNotificationCount) {}
