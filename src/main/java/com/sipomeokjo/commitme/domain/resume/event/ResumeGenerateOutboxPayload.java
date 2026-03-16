package com.sipomeokjo.commitme.domain.resume.event;

import java.util.List;

public record ResumeGenerateOutboxPayload(
        Long resumeVersionId, Long userId, String positionName, List<String> repoUrls) {}
