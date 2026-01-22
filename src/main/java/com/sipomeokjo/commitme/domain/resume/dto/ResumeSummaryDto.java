package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeSummaryDto {
    private final Long resumeId;
    private final String name;

    private final Long positionId;
    private final String positionName;

    private final Long companyId;        // company 없으면 null
    private final String companyName;    // company 없으면 null

    private final Integer currentVersionNo;
    private final LocalDateTime updatedAt;
}
