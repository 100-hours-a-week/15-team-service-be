package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.AnswerInputType;
import java.time.Instant;

public record InterviewMessageResponse(
        String id,
        Integer turnNo,
        String question,
        AnswerInputType answerInputType,
        String answer,
        Instant askedAt,
        Instant answeredAt) {}
