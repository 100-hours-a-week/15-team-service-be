package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiInterviewChatResponse(
        String status,
        String message,
        @JsonProperty("followUpQuestion") String followUpQuestion,
        @JsonProperty("turnNumber") Integer turnNumber,
        ErrorPayload error) {

    public record ErrorPayload(String code, String message) {}
}
