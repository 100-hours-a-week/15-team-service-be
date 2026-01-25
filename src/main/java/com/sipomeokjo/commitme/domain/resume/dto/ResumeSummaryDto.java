package com.sipomeokjo.commitme.domain.resume.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ResumeSummaryDto {
    private final Long resumeId;
    private final String name;

    private final Long positionId;
    private final String positionName;

    private final Long companyId;
    private final String companyName;

    private final Integer currentVersionNo;
    private final Instant updatedAt;
}
