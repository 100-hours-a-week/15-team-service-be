package com.sipomeokjo.commitme.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterviewUpdateNameRequest(
        @NotBlank(message = "면접 제목은 필수입니다.") @Size(max = 100, message = "면접 제목은 100자를 초과할 수 없습니다.")
                String name) {}
