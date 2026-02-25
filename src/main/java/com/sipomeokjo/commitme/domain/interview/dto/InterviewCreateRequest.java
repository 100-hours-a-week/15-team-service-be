package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import jakarta.validation.constraints.NotNull;

public record InterviewCreateRequest(
        @NotNull(message = "면접 유형은 필수입니다.") InterviewType interviewType,
        @NotNull(message = "포지션은 필수입니다.") Long positionId,
        Long companyId,
        Long resumeVersionId) {}
