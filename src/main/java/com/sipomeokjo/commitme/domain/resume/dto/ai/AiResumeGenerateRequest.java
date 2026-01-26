package com.sipomeokjo.commitme.domain.resume.dto.ai;

import java.util.List;

public record AiResumeGenerateRequest(
        List<String> repoUrls,
        String position,
        String githubToken,
        String callbackUrl
) {}
