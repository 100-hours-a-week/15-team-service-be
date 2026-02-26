package com.sipomeokjo.commitme.domain.interview.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.repository.InterviewRepository;
import com.sipomeokjo.commitme.domain.interview.service.InterviewCommandService;
import com.sipomeokjo.commitme.domain.interview.sse.InterviewSseEmitterManager;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewStreamController {

    private final InterviewSseEmitterManager sseEmitterManager;
    private final InterviewRepository interviewRepository;
    private final InterviewCommandService interviewCommandService;

    @GetMapping(value = "/{interviewId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUserId Long userId, @PathVariable Long interviewId) {
        Interview interview =
                interviewRepository
                        .findByIdAndUserId(interviewId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));

        if (interview.isEnded()) {
            throw new BusinessException(ErrorCode.INTERVIEW_ALREADY_ENDED);
        }

        SseEmitter emitter = sseEmitterManager.create(interviewId);
        interviewCommandService.sendNextQuestionIfAvailable(interviewId);
        return emitter;
    }
}
