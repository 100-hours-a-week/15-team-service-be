package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeEditResponse(
        Long resumeId, Integer versionNo, String name, String taskId, Instant updatedAt) {}
