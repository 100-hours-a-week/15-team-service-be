package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewCreateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewStartResponse;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewUpdateNameRequest;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.util.List;
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
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final InterviewAiService interviewAiService;

    public InterviewResponse updateName(
            Long userId, Long interviewId, InterviewUpdateNameRequest request) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        interview.updateName(request.name());

        return interviewMapper.toResponse(interview);
    }

    public void delete(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        // MongoDB 삭제 먼저 (트랜잭션 분리)
        interviewMessageRepository.deleteByInterviewId(interviewId);
        // MySQL 삭제
        interviewRepository.delete(interview);
    }

    public InterviewStartResponse create(Long userId, InterviewCreateRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Position position =
                positionRepository
                        .findById(request.positionId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));

        Company company = null;
        if (request.companyId() != null) {
            company =
                    companyRepository
                            .findById(request.companyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        String resumeContent = null;
        if (request.resumeVersionId() != null) {
            ResumeVersion resumeVersion =
                    resumeVersionRepository.findById(request.resumeVersionId()).orElse(null);
            if (resumeVersion != null) {
                resumeContent = resumeVersion.getContent();
            }
        }

        String interviewName = generateInterviewName(position.getName());

        Interview interview =
                Interview.create(user, position, company, interviewName, request.interviewType());

        interviewRepository.save(interview);

        interviewAiService.sendStartRequest(interview, resumeContent);

        return interviewMapper.toStartResponse(interview);
    }

    public void sendAnswer(Long userId, Long interviewId, InterviewAnswerRequest request) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        InterviewMessage message =
                interviewMessageRepository
                        .findByInterviewIdAndTurnNo(interviewId, request.turnNo())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID));

        message.updateAnswer(request.answer(), request.answerInputType(), request.audioUrl());
        interviewMessageRepository.save(message);

        interviewAiService.sendAnswerRequest(
                interview,
                request.turnNo(),
                request.answer(),
                request.answerInputType(),
                request.audioUrl());
    }

    public void end(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        interview.end();

        List<InterviewMessage> messages =
                interviewMessageRepository.findByInterviewIdOrderByTurnNoAsc(interviewId);

        interviewAiService.sendEndRequest(interview, messages);
    }

    private String generateInterviewName(String positionName) {
        return positionName + " 모의 면접";
    }
}
