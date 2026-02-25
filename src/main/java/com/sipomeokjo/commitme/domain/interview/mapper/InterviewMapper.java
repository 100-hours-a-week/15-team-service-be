package com.sipomeokjo.commitme.domain.interview.mapper;

import com.sipomeokjo.commitme.domain.interview.dto.InterviewDetailResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewMessageResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewStartResponse;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import org.springframework.stereotype.Component;

@Component
public class InterviewMapper {

    public InterviewResponse toResponse(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getName(),
                interview.getInterviewType(),
                interview.getPosition().getName(),
                interview.getCompany() != null ? interview.getCompany().getName() : null,
                interview.getStartedAt(),
                interview.getEndedAt(),
                interview.getCreatedAt());
    }

    public InterviewDetailResponse toDetailResponse(Interview interview) {
        return new InterviewDetailResponse(
                interview.getId(),
                interview.getName(),
                interview.getInterviewType(),
                interview.getPosition().getName(),
                interview.getCompany() != null ? interview.getCompany().getName() : null,
                interview.getTotalFeedback(),
                interview.getStartedAt(),
                interview.getEndedAt(),
                interview.getCreatedAt());
    }

    public InterviewMessageResponse toMessageResponse(InterviewMessage message) {
        return new InterviewMessageResponse(
                message.getId(),
                message.getTurnNo(),
                message.getQuestion(),
                message.getAnswerInputType(),
                message.getAnswer(),
                message.getAskedAt(),
                message.getAnsweredAt());
    }

    public InterviewStartResponse toStartResponse(Interview interview) {
        return new InterviewStartResponse(
                interview.getId(),
                interview.getAiSessionId(),
                interview.getName(),
                interview.getInterviewType(),
                interview.getPosition().getName(),
                interview.getCompany() != null ? interview.getCompany().getName() : null,
                interview.getStartedAt());
    }
}
