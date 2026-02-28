package com.sipomeokjo.commitme.domain.resume.event;

import java.time.Instant;

public record ResumeEditCompletedEvent(
        Long userId, Long resumeId, Integer versionNo, String taskId, Instant updatedAt) {}
