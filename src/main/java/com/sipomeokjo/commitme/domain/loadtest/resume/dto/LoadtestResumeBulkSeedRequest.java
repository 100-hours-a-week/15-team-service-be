package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeBulkSeedRequest(
        String runId,
        Integer userCount,
        Integer startIndex,
        Integer resumesPerUser,
        Integer succeededVersionsPerResume,
        Integer failedVersionsPerResume,
        Integer pendingVersionsPerResume,
        Long pendingStartedMinutesAgo,
        Long positionId,
        Boolean seedNotifications) {}
