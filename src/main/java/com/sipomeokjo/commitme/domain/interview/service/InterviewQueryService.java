package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewDetailResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewMessageResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewTypeResponse;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQueryService {

    private final InterviewRepository interviewRepository;
    private final InterviewMapper interviewMapper;
    private final InterviewMessageRepository interviewMessageRepository;

    public List<InterviewResponse> getInterviews(Long userId) {
        List<Interview> interviews = interviewRepository.findAllByUserIdWithDetails(userId);
        return interviews.stream().map(interviewMapper::toResponse).toList();
    }

    public InterviewDetailResponse getInterview(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserIdWithDetails(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        return interviewMapper.toDetailResponse(interview);
    }

    public List<InterviewTypeResponse> getInterviewTypes() {
        return Arrays.stream(InterviewType.values()).map(InterviewTypeResponse::from).toList();
    }

    public List<InterviewMessageResponse> getInterviewMessages(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        return interviewMessageRepository
                .findByInterviewIdAndTurnNoIsNotNullOrderByTurnNoAsc(interview.getId())
                .stream()
                .map(interviewMapper::toMessageResponse)
                .toList();
    }
}
