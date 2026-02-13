package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewUpdateNameRequest;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewCommandService {

    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewMapper interviewMapper;

    public InterviewResponse updateName(Long userId, Long interviewId, InterviewUpdateNameRequest request) {
        Interview interview = interviewRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        interview.updateName(request.name());

        return interviewMapper.toResponse(interview);
    }

    public void delete(Long userId, Long interviewId) {
        Interview interview = interviewRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        interviewMessageRepository.deleteAllByInterviewId(interviewId);
        interviewRepository.delete(interview);
    }
}
