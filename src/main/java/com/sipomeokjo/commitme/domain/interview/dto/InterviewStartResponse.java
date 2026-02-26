package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import java.time.Instant;

public record InterviewStartResponse(
        Long id,
        String aiSessionId,
        String name,
        InterviewType interviewType,
        String positionName,
        String companyName,
        Instant startedAt) {}
