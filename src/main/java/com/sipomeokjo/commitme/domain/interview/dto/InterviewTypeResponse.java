package com.sipomeokjo.commitme.domain.interview.dto;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;

public record InterviewTypeResponse(InterviewType type, String label) {

    public static InterviewTypeResponse from(InterviewType type) {
        String label =
                switch (type) {
                    case TECHNICAL -> "기술 면접";
                    case BEHAVIORAL -> "인성 면접";
                };
        return new InterviewTypeResponse(type, label);
    }
}
