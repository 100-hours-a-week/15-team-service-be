package com.sipomeokjo.commitme.dto.resume;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeSummaryDto {
    private final Long resumeId;
    private final String name;
    private final String position;
    private final String company;
    private final LocalDateTime lastModifiedAt;


}
