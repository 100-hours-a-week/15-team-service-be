package com.sipomeokjo.commitme.domain.resume.event;

import java.util.List;

public record ResumeGenerateOutboxPayload(
        Long resumeId,
        Integer versionNo,
        Long userId,
        String positionName,
        List<String> repoUrls) {}
