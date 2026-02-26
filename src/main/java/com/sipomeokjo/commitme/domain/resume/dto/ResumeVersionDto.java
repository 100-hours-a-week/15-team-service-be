package com.sipomeokjo.commitme.domain.resume.dto;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeVersionDto {
    private final Long resumeId;
    private final Integer versionNo;
    private final ResumeVersionStatus status;

    private final String content;
    private final String aiTaskId;
    private final String errorLog;

    private final Instant startedAt;
    private final Instant finishedAt;
    private final Instant committedAt;

    private final Instant createdAt;
    private final Instant updatedAt;
}
