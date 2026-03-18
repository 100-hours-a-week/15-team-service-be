package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeForceCompleteResponse(
        String runId,
        int targetVersionCount,
        int completedVersionCount,
        int skippedCount,
        String resultStatus) {}
