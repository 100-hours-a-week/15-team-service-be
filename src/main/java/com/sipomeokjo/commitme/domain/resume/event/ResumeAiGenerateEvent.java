package com.sipomeokjo.commitme.domain.resume.event;

import java.util.List;

public record ResumeAiGenerateEvent(
        Long resumeVersionId, Long userId, String positionName, List<String> repoUrls) {}
