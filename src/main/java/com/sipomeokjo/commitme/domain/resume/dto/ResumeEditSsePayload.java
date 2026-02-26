package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeEditSsePayload(
        Long resumeId, Integer versionNo, String taskId, Instant updatedAt, Object resume) {}
