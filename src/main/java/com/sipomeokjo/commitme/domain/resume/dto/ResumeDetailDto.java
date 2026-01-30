package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeDetailDto {
    private final Long resumeId;
    private final String name;

    private final Long positionId;
    private final String positionName;

    private final Long companyId;
    private final String companyName;

    private final Integer currentVersionNo;
    private final String content;

    private final Instant createdAt;
    private final Instant updatedAt;
}
