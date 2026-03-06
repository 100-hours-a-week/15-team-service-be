package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileCreateResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileUpdateResponse;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProfileService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeProfileService resumeProfileService;

    @GetMapping
    public ResponseEntity<APIResponse<CursorResponse<ResumeSummaryDto>>> list(
            @CurrentUserId Long userId,
            CursorRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "UPDATED_DESC") String sortedBy) {

        return APIResponse.onSuccess(
                SuccessCode.RESUME_LIST_FETCHED,
                resumeService.list(userId, request, keyword, sortedBy));
    }

    @PostMapping
    public ResponseEntity<APIResponse<Long>> create(
            @CurrentUserId Long userId, @RequestBody ResumeCreateRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_CREATED, resumeService.create(userId, request));
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<APIResponse<ResumeDetailDto>> get(
            @CurrentUserId Long userId, @PathVariable Long resumeId) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_FETCHED, resumeService.get(userId, resumeId));
    }

    @GetMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<ResumeVersionDto>> getVersion(
            @CurrentUserId Long userId, @PathVariable Long resumeId, @PathVariable int versionNo) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_VERSION_FETCHED,
                resumeService.getVersion(userId, resumeId, versionNo));
    }

    @PatchMapping("/{resumeId}/name")
    public ResponseEntity<APIResponse<Void>> rename(
            @CurrentUserId Long userId,
            @PathVariable Long resumeId,
            @RequestBody ResumeRenameRequest request) {

        resumeService.rename(userId, resumeId, request);
        return APIResponse.onSuccess(SuccessCode.RESUME_NAME_UPDATED);
    }

    @PatchMapping("/{resumeId}")
    public ResponseEntity<APIResponse<ResumeEditResponse>> edit(
            @CurrentUserId Long userId,
            @PathVariable Long resumeId,
            @RequestBody ResumeEditRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_EDIT_REQUESTED, resumeService.edit(userId, resumeId, request));
    }

    @GetMapping("/profile")
    public ResponseEntity<APIResponse<ResumeProfileResponse>> getProfile(
            @CurrentUserId Long userId) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_PROFILE_FETCHED, resumeProfileService.getProfile(userId));
    }

    @GetMapping("/{resumeId}/profile")
    public ResponseEntity<APIResponse<ResumeProfileResponse>> getSpecificProfile(
            @CurrentUserId Long userId, @PathVariable Long resumeId) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_PROFILE_FETCHED,
                resumeProfileService.getProfile(userId, resumeId));
    }

    @PostMapping("/profile")
    public ResponseEntity<APIResponse<ResumeProfileCreateResponse>> createProfile(
            @CurrentUserId Long userId, @RequestBody ResumeProfileRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_PROFILE_CREATED,
                resumeProfileService.createProfile(userId, request));
    }

    @PutMapping("/profile")
    public ResponseEntity<APIResponse<ResumeProfileUpdateResponse>> updateDefaultProfile(
            @CurrentUserId Long userId, @RequestBody ResumeProfileRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_PROFILE_UPDATED,
                resumeProfileService.updateDefaultProfile(userId, request));
    }

    @PutMapping("/{resumeId}/profile")
    public ResponseEntity<APIResponse<ResumeProfileUpdateResponse>> updateProfile(
            @CurrentUserId Long userId,
            @PathVariable Long resumeId,
            @RequestBody ResumeProfileRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.RESUME_PROFILE_UPDATED,
                resumeProfileService.updateProfile(userId, resumeId, request));
    }

    @PostMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<Void>> saveVersion(
            @CurrentUserId Long userId, @PathVariable Long resumeId, @PathVariable int versionNo) {
        resumeService.saveVersion(userId, resumeId, versionNo);
        return APIResponse.onSuccess(SuccessCode.RESUME_VERSION_SAVED);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<APIResponse<Void>> delete(
            @CurrentUserId Long userId, @PathVariable Long resumeId) {
        resumeService.delete(userId, resumeId);
        return APIResponse.onSuccess(SuccessCode.RESUME_DELETED);
    }
}
