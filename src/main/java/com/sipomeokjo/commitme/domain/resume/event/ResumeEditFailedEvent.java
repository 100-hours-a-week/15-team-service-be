package com.sipomeokjo.commitme.domain.resume.event;

import java.time.Instant;

public record ResumeEditFailedEvent(
        Long userId,
        Long resumeId,
        Integer versionNo,
        String taskId,
        Instant updatedAt,
        String errorCode,
        String errorMessage) {}
