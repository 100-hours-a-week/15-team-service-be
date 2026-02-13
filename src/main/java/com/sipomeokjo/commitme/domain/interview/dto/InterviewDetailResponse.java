package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import java.time.Instant;

public record InterviewDetailResponse(
        Long id,
        String name,
        InterviewType interviewType,
        String positionName,
        String companyName,
        String totalFeedback,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt) {}
