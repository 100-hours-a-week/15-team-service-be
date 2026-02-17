package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeSseService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resumes")
@Slf4j
public class ResumeSseController {
    private final ResumeSseService resumeSseService;
    private final ResumeService resumeService;

    @GetMapping(value = "/{resumeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@CurrentUserId Long userId, @PathVariable Long resumeId) {
        boolean exists = resumeService.existsByResumeIdAndUserId(resumeId, userId);

        if (!exists) {
            log.warn("[RESUME_SSE] subscribe_not_found userId={} resumeId={}", userId, resumeId);
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        return resumeSseService.subscribe(resumeId);
    }
}
