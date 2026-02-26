package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiInterviewGenerateResponse(
        String status,
        @JsonProperty("aiSessionId") String aiSessionId,
        List<QuestionPayload> questions,
        ErrorPayload error) {

    public record QuestionPayload(@JsonProperty("questionId") String questionId, String text) {}

    public record ErrorPayload(String code, String message) {}
}
