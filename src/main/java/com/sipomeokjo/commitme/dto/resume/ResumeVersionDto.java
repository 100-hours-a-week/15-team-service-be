package com.sipomeokjo.commitme.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ResumeVersionDto {
    private final Long resumeId;
    private final int versionNo;
    private final String status;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime commitedAt;
}
