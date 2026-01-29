package com.sipomeokjo.commitme.domain.resume.dto.ai;

import java.util.List;

public record AiResumeCallbackRequest(
        String jobId,
        String status, // "success" | "failed"
        ResumePayload resume, // success일 때만
        ErrorPayload error // failed일 때만
        ) {

    public record ResumePayload(List<ProjectPayload> projects) {}

    public record ProjectPayload(
            String name, String repoUrl, String description, List<String> techStack) {}

    public record ErrorPayload(String code, String message) {}
}
