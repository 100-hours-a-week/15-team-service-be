package com.sipomeokjo.commitme.domain.resume.dto;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.LocalDateTime;
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

    private final LocalDateTime startedAt;
    private final LocalDateTime finishedAt;
    private final LocalDateTime committedAt;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
