package com.sipomeokjo.commitme.domain.resume.event;

import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import java.time.Instant;

public record ResumeEditCompletedEvent(
        Long resumeId,
        Integer versionNo,
        String taskId,
        Instant updatedAt,
        AiResumeCallbackRequest.ResumePayload content) {}
