package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

public record LoadtestResumeCallbackReplayResponse(
        String runId,
        int targetVersionCount,
        int replayedCallbackCount,
        int duplicateCount,
        LoadtestResumeReplayResultStatus resultStatus,
        LoadtestResumeCallbackType callbackType) {}
