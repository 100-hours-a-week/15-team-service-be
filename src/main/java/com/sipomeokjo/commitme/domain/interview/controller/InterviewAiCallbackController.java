package com.sipomeokjo.commitme.domain.interview.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewFeedbackCallback;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewMessageCallback;
import com.sipomeokjo.commitme.domain.interview.service.InterviewAiCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/callback/interview")
@RequiredArgsConstructor
public class InterviewAiCallbackController {

    private final InterviewAiCallbackService callbackService;

    @PostMapping("/message")
    public ResponseEntity<APIResponse<Void>> onMessage(
            @RequestBody AiInterviewMessageCallback callback) {
        callbackService.handleMessageCallback(callback);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_CALLBACK_RECEIVED);
    }

    @PostMapping("/feedback")
    public ResponseEntity<APIResponse<Void>> onFeedback(
            @RequestBody AiInterviewFeedbackCallback callback) {
        callbackService.handleFeedbackCallback(callback);
        return APIResponse.onSuccess(SuccessCode.INTERVIEW_CALLBACK_RECEIVED);
    }
}
