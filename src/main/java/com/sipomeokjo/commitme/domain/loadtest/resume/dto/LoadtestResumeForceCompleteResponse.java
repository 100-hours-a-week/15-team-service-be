package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeForceCompleteResponse(
        String runId,
        int targetResumeCount,
        int completedEventCount,
        LoadtestResumeReplayResultStatus resultStatus) {}
