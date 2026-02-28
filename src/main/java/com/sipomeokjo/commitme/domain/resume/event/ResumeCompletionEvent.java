package com.sipomeokjo.commitme.domain.resume.event;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Instant;

public record ResumeCompletionEvent(
        Long userId,
        Long resumeId,
        Integer versionNo,
        String taskId,
        Instant updatedAt,
        ResumeVersionStatus status,
        ResumeCallbackSource source,
        String errorCode,
        String errorMessage) {}
