package com.sipomeokjo.commitme.domain.loadtest.resume.dto;

import java.util.List;

public record LoadtestResumeForceCompleteRequest(
        String runId,
        List<Long> resumeIds,
        Integer limit,
        LoadtestResumeReplayResultStatus resultStatus) {}
