package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeEditFailedSsePayload(
        Long resumeId,
        Integer versionNo,
        String taskId,
        Instant updatedAt,
        String errorCode,
        String errorMessage) {}
