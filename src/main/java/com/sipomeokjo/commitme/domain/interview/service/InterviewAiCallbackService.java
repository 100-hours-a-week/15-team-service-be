package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewFeedbackCallback;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewMessageCallback;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewMessageRepository;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import com.sipomeokjo.commitme.domain.interview.sse.InterviewSseEmitterManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewAiCallbackService {

    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final InterviewSseEmitterManager sseEmitterManager;

    public void handleMessageCallback(AiInterviewMessageCallback callback) {
        Interview interview =
                interviewRepository
                        .findByAiSessionId(callback.aiSessionId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID));

        if (interview.isEnded()) {
            log.warn("Interview already ended: {}", callback.aiSessionId());
            return;
        }

        InterviewMessage message =
                InterviewMessage.createFromQuestion(
                        interview.getId(),
                        callback.turnNo(),
                        callback.question(),
                        callback.askedAt());

        interviewMessageRepository.save(message);

        sseEmitterManager.sendQuestion(
                interview.getId(),
                Map.of(
                        "turnNo", callback.turnNo(),
                        "question", callback.question(),
                        "askedAt", callback.askedAt().toString()));

        log.info(
                "Question callback processed: {}, turnNo: {}",
                callback.aiSessionId(),
                callback.turnNo());
    }

    public void handleFeedbackCallback(AiInterviewFeedbackCallback callback) {
        Interview interview =
                interviewRepository
                        .findByAiSessionId(callback.aiSessionId())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.INTERVIEW_SESSION_INVALID));

        interview.updateFeedback(callback.totalFeedback());

        sseEmitterManager.sendFeedback(
                interview.getId(), Map.of("totalFeedback", callback.totalFeedback()));

        sseEmitterManager.sendEnd(interview.getId());

        log.info("Feedback callback processed: {}", callback.aiSessionId());
    }
}
