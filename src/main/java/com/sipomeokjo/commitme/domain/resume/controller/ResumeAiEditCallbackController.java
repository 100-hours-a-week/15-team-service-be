package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackResult;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeSseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/resume")
public class ResumeAiEditCallbackController {

    private final ResumeAiCallbackService resumeAiCallbackService;
    private final ResumeSseService resumeSseService;
    private final AiProperties aiProperties;

    @PostMapping("/{jobId}/callback")
    public ResponseEntity<APIResponse<Void>> callback(
            @RequestHeader(value = "X-AI-Callback-Secret", required = false) String secret,
            @PathVariable String jobId,
            HttpServletRequest request,
            @RequestBody AiResumeCallbackRequest req) {
        boolean secretMatches = secret != null && secret.equals(aiProperties.getCallbackSecret());

        if (!secretMatches) {
            log.warn(
                    "[AI_CALLBACK] unauthorized uri={} remote={} pathJobId={} bodyJobId={} status={} secretPresent={} secretMatches={}",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    jobId,
                    req == null ? null : req.jobId(),
                    req == null ? null : req.status(),
                    secret != null,
                    false);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (req == null || req.jobId() == null || !req.jobId().equals(jobId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        ResumeAiCallbackResult result = resumeAiCallbackService.handleCallback(req);
        if (result.updated()
                && result.status() == ResumeVersionStatus.SUCCEEDED
                && req.content() != null) {
            resumeSseService.sendEditCompleted(
                    result.resumeId(),
                    result.versionNo(),
                    result.taskId(),
                    result.updatedAt(),
                    req.content());
        }

        if (result.updated() && result.status() == ResumeVersionStatus.FAILED) {
            resumeSseService.sendEditFailed(
                    result.resumeId(),
                    result.versionNo(),
                    result.taskId(),
                    result.updatedAt(),
                    (req.error() == null
                                    || req.error().code() == null
                                    || req.error().code().isBlank())
                            ? "AI_FAILED"
                            : req.error().code(),
                    (req.error() == null || req.error().message() == null)
                            ? "unknown"
                            : req.error().message());
            log.warn(
                    "[AI_CALLBACK] sse_failed_dispatched resumeId={} versionNo={} taskId={}",
                    result.resumeId(),
                    result.versionNo(),
                    result.taskId());
        }

        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
