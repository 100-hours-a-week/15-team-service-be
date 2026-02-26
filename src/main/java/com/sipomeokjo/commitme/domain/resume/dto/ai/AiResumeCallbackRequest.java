package com.sipomeokjo.commitme.domain.resume.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AiResumeCallbackRequest(
        String jobId,
        String status,
        @JsonAlias("resume") ResumePayload content,
        ErrorPayload error) {

    public record ResumePayload(List<String> techStack, List<ProjectPayload> projects) {}

    public record ProjectPayload(
            String name, String repoUrl, String description, List<String> techStack) {}

    public record ErrorPayload(String code, String message) {}
}
