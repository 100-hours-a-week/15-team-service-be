package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

import java.util.List;

public record LoadtestResumeCallbackReplayRequest(
        String runId,
        List<Long> versionIds,
        Integer limit,
        Integer duplicateCount,
        LoadtestResumeReplayResultStatus resultStatus,
        LoadtestResumeCallbackType callbackType) {}
