package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiInterviewChatRequest(
        @JsonProperty("aiSessionId") String aiSessionId,
        @JsonProperty("questionId") String questionId,
        String answer) {}
