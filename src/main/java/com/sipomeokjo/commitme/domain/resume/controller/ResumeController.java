package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<APIResponse<CursorResponse<ResumeSummaryDto>>> list(
            @CurrentUserId Long userId,
            CursorRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "UPDATED_DESC") String sortedBy) {

        return APIResponse.onSuccess(
                SuccessCode.OK, resumeService.list(userId, request, keyword, sortedBy));
    }

    @PostMapping
    public ResponseEntity<APIResponse<Long>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ResumeCreateRequest request) {
        Long userId = principal.userId();
        Long resumeId = resumeService.create(userId, request);
        return APIResponse.onSuccess(SuccessCode.CREATED, resumeId);
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<APIResponse<ResumeDetailDto>> get(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long resumeId) {
        Long userId = principal.userId();
        ResumeDetailDto data = resumeService.get(userId, resumeId);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }

    @GetMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<ResumeVersionDto>> getVersion(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @PathVariable int versionNo) {
        Long userId = principal.userId();
        ResumeVersionDto data = resumeService.getVersion(userId, resumeId, versionNo);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }

    @PatchMapping("/{resumeId}/name")
    public ResponseEntity<APIResponse<Void>> rename(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @RequestBody ResumeRenameRequest request) {
        Long userId = principal.userId();
        resumeService.rename(userId, resumeId, request);
        return APIResponse.onSuccess(SuccessCode.OK);
    }

    @PostMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<Void>> saveVersion(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @PathVariable int versionNo) {
        Long userId = principal.userId();
        resumeService.saveVersion(userId, resumeId, versionNo);
        return APIResponse.onSuccess(SuccessCode.OK);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<APIResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long resumeId) {
        Long userId = principal.userId();
        resumeService.delete(userId, resumeId);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
