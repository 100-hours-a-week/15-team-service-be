package com.sipomeokjo.commitme.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ResumeDetailDto {
    private final Long resumeId;
    private final Long name;
    private final String position;
    private final String company;
    private final int currentVersionNo;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
