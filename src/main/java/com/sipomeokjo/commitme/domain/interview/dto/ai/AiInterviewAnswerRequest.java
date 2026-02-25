package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.sipomeokjo.commitme.domain.interview.entity.AnswerInputType;
import java.time.Instant;

public record AiInterviewAnswerRequest(
        String aiSessionId,
        Integer turnNo,
        String answer,
        AnswerInputType answerInputType,
        String audioUrl,
        Instant answeredAt) {}
