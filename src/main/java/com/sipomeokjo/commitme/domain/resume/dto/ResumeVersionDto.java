package com.sipomeokjo.commitme.domain.resume.dto;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ResumeVersionDto {
    private final Long resumeId;
    private final Integer versionNo;
    private final ResumeVersionStatus status;

    private final String content;
    private final String aiTaskId;
    private final String errorLog;

    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final LocalDateTime committedAt;

    private final Instant createdAt;
    private final Instant updatedAt;
}
