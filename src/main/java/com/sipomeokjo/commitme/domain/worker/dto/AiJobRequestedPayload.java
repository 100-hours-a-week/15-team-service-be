package com.sipomeokjo.commitme.domain.worker.dto;

import java.util.List;

public record AiJobRequestedPayload(
        Long resumeVersionId, Long userId, String positionName, List<String> repoUrls) {}
