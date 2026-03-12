package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeBulkSeedResponse(
        String runId,
        int requestedUserCount,
        int processedUserCount,
        int createdResumeCount,
        int createdVersionCount,
        int createdNotificationCount) {}
