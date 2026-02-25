package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiInterviewGenerateRequest(
        Integer resumeId, ResumeContent content, String type, String position) {

    public record ResumeContent(List<ProjectPayload> projects) {}

    public record ProjectPayload(
            String name,
            @JsonProperty("repoUrl") String repoUrl,
            @JsonProperty("techStack") List<String> techStack,
            String description) {}
}
