package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.AnswerInputType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewAnswerRequest(
        @NotNull(message = "질문 순서는 필수입니다.") Integer turnNo,
        @NotBlank(message = "답변은 필수입니다.") String answer,
        @NotNull(message = "답변 입력 타입은 필수입니다.") AnswerInputType answerInputType,
        String audioUrl) {}
