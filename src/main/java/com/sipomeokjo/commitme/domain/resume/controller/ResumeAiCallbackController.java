package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume")
public class ResumeAiCallbackController {

    private final ResumeAiCallbackService resumeAiCallbackService;
    private final AiProperties aiProperties;

    @PostMapping("/callback")
    public ResponseEntity<APIResponse<Void>> callback(
            @RequestHeader(value = "X-AI-Callback-Secret", required = false) String secret,
            HttpServletRequest request,
            @RequestBody AiResumeCallbackRequest req) {
        boolean secretPresent = secret != null;
        boolean secretMatches = secretPresent && secret.equals(aiProperties.getCallbackSecret());

        log.info(
                "[AI_CALLBACK] received uri={} remote={} jobId={} status={} secretPresent={} secretMatches={}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                req == null ? null : req.jobId(),
                req == null ? null : req.status(),
                secretPresent,
                secretMatches);

        if (!secretMatches) {
            log.warn(
                    "[AI_CALLBACK] unauthorized uri={} remote={} jobId={} status={} secretPresent={} secretMatches={}",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    req == null ? null : req.jobId(),
                    req == null ? null : req.status(),
                    secretPresent,
                    secretMatches);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        resumeAiCallbackService.handleCallback(req);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
