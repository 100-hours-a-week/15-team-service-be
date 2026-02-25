package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;

public record AiInterviewStartRequest(
        String aiSessionId,
        InterviewType interviewType,
        String position,
        String company,
        String resumeContent,
        String callbackUrl) {}
