package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;

public record ResumeDetailDto(
        Long resumeId,
        String name,
        boolean isEditing,
        Long positionId,
        String positionName,
        Long companyId,
        String companyName,
        Integer currentVersionNo,
        String content,
        Instant commitedAt,
        Instant createdAt,
        Instant updatedAt) {}
