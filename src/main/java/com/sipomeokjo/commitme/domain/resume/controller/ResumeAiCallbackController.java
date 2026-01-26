package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
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

    @PostMapping("/callback")
    public ResponseEntity<APIResponse<Void>> callback(@RequestBody AiResumeCallbackRequest req) {
        resumeAiCallbackService.handleCallback(req);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
