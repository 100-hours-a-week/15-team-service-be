package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume")
public class ResumeAiCallbackController {

    private final ResumeAiCallbackService resumeAiCallbackService;
    private final AiProperties aiProperties;

    @PostMapping("/callback")
    public ResponseEntity<APIResponse<Void>> callback(
            @RequestHeader(value = "X-AI-Callback-Secret", required = false) String secret,
            @RequestBody AiResumeCallbackRequest req
    ) {
        if (secret == null || !secret.equals(aiProperties.getCallbackSecret())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        resumeAiCallbackService.handleCallback(req);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
