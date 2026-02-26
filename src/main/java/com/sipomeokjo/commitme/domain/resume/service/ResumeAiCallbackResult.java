package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Instant;

public record ResumeAiCallbackResult(
        Long resumeId,
        Integer versionNo,
        String taskId,
        ResumeVersionStatus status,
        Instant updatedAt,
        boolean updated) {}
