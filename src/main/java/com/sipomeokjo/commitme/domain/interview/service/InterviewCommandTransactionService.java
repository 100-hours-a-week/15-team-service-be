package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewCreateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.InterviewResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import com.sipomeokjo.commitme.domain.interview.mapper.InterviewMapper;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.service.UserFinder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewCommandTransactionService {

    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewMapper interviewMapper;
    private final UserFinder userFinder;
    private final PositionFinder positionFinder;
    private final CompanyRepository companyRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public InterviewResponse updateName(Long userId, Long interviewId, String name) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        interview.updateName(name);
        interviewRepository.save(interview);
        return interviewMapper.toResponse(interview);
    }

    @Transactional(readOnly = true)
    public void validateOwnership(Long userId, Long interviewId) {
        interviewRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
    }

    @Transactional
    public void deleteInterview(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        interviewRepository.delete(interview);
    }

    @Transactional(readOnly = true)
    public CreatePrepared prepareCreate(Long userId, InterviewCreateRequest request) {
        User user = userFinder.getByIdOrThrow(userId);
        Position position = positionFinder.getByIdOrThrow(request.positionId());

        Company company = null;
        if (request.companyId() != null) {
            company =
                    companyRepository
                            .findById(request.companyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        ResumeVersion resumeVersion = resolveResumeVersion(request);
        if (resumeVersion == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String companyNameForAi = resolveCompanyName(company, request.companyName());
        String interviewName = generateInterviewName(position.getName(), request.interviewType());

        return new CreatePrepared(
                user.getId(),
                position.getId(),
                company == null ? null : company.getId(),
                interviewName,
                request.interviewType(),
                position.getName(),
                companyNameForAi,
                resumeVersion.getResume().getId(),
                resumeVersion.getContent());
    }

    @Transactional
    public Interview createInterview(
            CreatePrepared prepared, AiInterviewGenerateResponse aiResponse) {
        User user = userFinder.getByIdOrThrow(prepared.userId());
        Position position = positionFinder.getByIdOrThrow(prepared.positionId());
        Company company = null;
        if (prepared.companyId() != null) {
            company =
                    companyRepository
                            .findById(prepared.companyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        Interview interview =
                Interview.create(
                        user,
                        position,
                        company,
                        prepared.interviewName(),
                        prepared.interviewType());
        if (prepared.resumeId() != null) {
            interview.updateResume(resumeRepository.getReferenceById(prepared.resumeId()));
        }
        interviewRepository.save(interview);
        interview.updateAiSessionId(aiResponse.aiSessionId());

        List<InterviewMessage> questionMessages = new ArrayList<>();
        int order = 1;
        for (AiInterviewGenerateResponse.QuestionPayload question : aiResponse.questions()) {
            questionMessages.add(
                    InterviewMessage.createFromGeneratedQuestion(
                            interview.getId(), order, question.questionId(), question.text()));
            order++;
        }
        interviewMessageRepository.saveAll(questionMessages);
        return interview;
    }

    @Transactional
    public AnswerPrepared prepareAnswer(
            Long userId, Long interviewId, InterviewAnswerRequest request) {
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

        if (message.getQuestionId() == null || message.getQuestionId().isBlank()) {
            throw new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID);
        }

        return new AnswerPrepared(
                interview.getId(),
                interview.getAiSessionId(),
                message.getQuestionId(),
                request.answer());
    }

    @Transactional
    public InterviewQuestionDispatch advanceAfterChat(
            Long interviewId, AiInterviewChatResponse chatResponse) {
        String followUpQuestion = chatResponse.followUpQuestion();
        if (followUpQuestion != null && !followUpQuestion.isBlank()) {
            InterviewMessage latestAsked =
                    interviewMessageRepository
                            .findFirstByInterviewIdAndTurnNoIsNotNullOrderByTurnNoDesc(interviewId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.INTERVIEW_SESSION_INVALID));
            Integer nextTurnNo = latestAsked.getTurnNo() + 1;
            Instant askedAt = Instant.now();
            InterviewMessage followUpMessage =
                    InterviewMessage.createFollowUpQuestion(
                            interviewId,
                            nextTurnNo,
                            latestAsked.getQuestionId(),
                            followUpQuestion,
                            askedAt);
            interviewMessageRepository.save(followUpMessage);
            return InterviewQuestionDispatch.question(nextTurnNo, followUpQuestion, askedAt);
        }

        return advanceNextQuestion(interviewId);
    }

    @Transactional(readOnly = true)
    public EndPrepared prepareEnd(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserIdWithResume(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        Long resumeId = interview.getResume() != null ? interview.getResume().getId() : null;
        String profileSnapshot =
                interview.getResume() != null ? interview.getResume().getProfileSnapshot() : null;

        return new EndPrepared(
                interview.getId(),
                interview.getAiSessionId(),
                interview.getInterviewType(),
                interview.getPosition().getName(),
                interview.getCompany() != null ? interview.getCompany().getName() : "미지정",
                resumeId,
                profileSnapshot);
    }

    @Transactional
    public void markEnded(Long userId, Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }
        interview.end();
    }

    @Transactional
    public String applyFeedback(Long interviewId, String feedbackJson) {
        Interview interview =
                interviewRepository
                        .findById(interviewId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        interview.updateFeedback(feedbackJson);
        return interview.getTotalFeedback();
    }

    @Transactional
    public InterviewQuestionDispatch prepareNextQuestion(Long interviewId) {
        return advanceNextQuestion(interviewId);
    }

    private InterviewQuestionDispatch advanceNextQuestion(Long interviewId) {
        InterviewMessage latestAsked =
                interviewMessageRepository
                        .findFirstByInterviewIdAndTurnNoIsNotNullOrderByTurnNoDesc(interviewId)
                        .orElse(null);

        if (latestAsked != null
                && (latestAsked.getAnswer() == null || latestAsked.getAnswer().isBlank())) {
            return InterviewQuestionDispatch.question(
                    latestAsked.getTurnNo(), latestAsked.getQuestion(), latestAsked.getAskedAt());
        }

        InterviewMessage nextQuestion =
                interviewMessageRepository
                        .findFirstByInterviewIdAndQuestionOrderIsNotNullAndAskedAtIsNullOrderByQuestionOrderAsc(
                                interviewId)
                        .orElse(null);
        if (nextQuestion == null) {
            return InterviewQuestionDispatch.complete();
        }

        Integer nextTurnNo =
                interviewMessageRepository
                        .findFirstByInterviewIdAndTurnNoIsNotNullOrderByTurnNoDesc(interviewId)
                        .map(msg -> msg.getTurnNo() + 1)
                        .orElse(1);
        Instant askedAt = Instant.now();
        nextQuestion.markAsked(nextTurnNo, askedAt);
        interviewMessageRepository.save(nextQuestion);

        return InterviewQuestionDispatch.question(nextTurnNo, nextQuestion.getQuestion(), askedAt);
    }

    private ResumeVersion resolveResumeVersion(InterviewCreateRequest request) {
        if (request.resumeVersionId() != null) {
            return resumeVersionRepository.findById(request.resumeVersionId()).orElse(null);
        }
        if (request.resumeId() != null && request.resumeVersionNo() != null) {
            return resumeVersionRepository
                    .findByResume_IdAndVersionNo(request.resumeId(), request.resumeVersionNo())
                    .orElse(null);
        }
        if (request.resumeId() != null) {
            return resumeVersionRepository
                    .findTopByResume_IdAndStatusOrderByVersionNoDesc(
                            request.resumeId(), ResumeVersionStatus.SUCCEEDED)
                    .orElse(null);
        }
        return null;
    }

    private String generateInterviewName(String positionName, InterviewType interviewType) {
        String date = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString();
        String typeLabel =
                interviewType == null
                        ? "면접"
                        : switch (interviewType) {
                            case BEHAVIORAL -> "인성";
                            case TECHNICAL -> "기술";
                        };
        return String.join("_", date, positionName, typeLabel);
    }

    private String resolveCompanyName(Company company, String companyName) {
        if (company != null) {
            return company.getName();
        }
        if (companyName != null && !companyName.isBlank()) {
            return companyName;
        }
        return null;
    }

    public record CreatePrepared(
            Long userId,
            Long positionId,
            Long companyId,
            String interviewName,
            InterviewType interviewType,
            String positionName,
            String companyName,
            Long resumeId,
            String resumeContent) {}

    public record AnswerPrepared(
            Long interviewId, String aiSessionId, String questionId, String answer) {}

    public record EndPrepared(
            Long interviewId,
            String aiSessionId,
            InterviewType interviewType,
            String positionName,
            String companyName,
            Long resumeId,
            String profileSnapshot) {}

    public record InterviewQuestionDispatch(
            boolean completed, Integer turnNo, String question, Instant askedAt) {
        static InterviewQuestionDispatch question(
                Integer turnNo, String question, Instant askedAt) {
            return new InterviewQuestionDispatch(false, turnNo, question, askedAt);
        }

        static InterviewQuestionDispatch complete() {
            return new InterviewQuestionDispatch(true, null, null, null);
        }
    }
}
