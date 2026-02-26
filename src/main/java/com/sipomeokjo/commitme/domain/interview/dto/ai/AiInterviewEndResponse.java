package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiInterviewEndResponse(
        String status,
        List<FeedbackItem> feedbacks,
        @JsonProperty("overallFeedback") OverallFeedback overallFeedback,
        ErrorPayload error) {

    public record FeedbackItem(
            @JsonProperty("turnNo") Integer turnNo,
            Integer score,
            List<String> strengths,
            List<String> improvements,
            @JsonProperty("modelAnswer") String modelAnswer) {}

    public record OverallFeedback(
            @JsonProperty("overallScore") Integer overallScore,
            String summary,
            @JsonProperty("keyStrengths") List<String> keyStrengths,
            @JsonProperty("keyImprovements") List<String> keyImprovements) {}

    public record ErrorPayload(String code, String message) {}
}
