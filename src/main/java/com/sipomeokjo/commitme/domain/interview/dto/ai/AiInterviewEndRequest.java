package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import java.util.List;

public record AiInterviewEndRequest(
        String aiSessionId,
        InterviewType interviewType,
        String position,
        String company,
        List<MessagePayload> messages) {

    public record MessagePayload(
            Integer turnNo,
            String question,
            String answer,
            String answerInputType,
            String askedAt,
            String answeredAt) {}
}
