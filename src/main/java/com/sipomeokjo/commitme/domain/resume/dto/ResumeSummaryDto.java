package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeSummaryDto(
        Long resumeId,
        String name,
        Long positionId,
        String positionName,
        Long companyId,
        String companyName,
        Integer currentVersionNo,
        Instant updatedAt) {}
