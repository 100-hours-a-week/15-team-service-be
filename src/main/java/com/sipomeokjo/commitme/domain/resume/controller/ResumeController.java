package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.pagination.PagingResponse;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.security.CustomUserDetails;
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
    public ResponseEntity<APIResponse<PagingResponse<ResumeSummaryDto>>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = principal.userId();
        PagingResponse<ResumeSummaryDto> data = resumeService.list(userId, page, size);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }

    @PostMapping
    public ResponseEntity<APIResponse<Long>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ResumeCreateRequest request
    ) {
        Long userId = principal.userId();
        Long resumeId = resumeService.create(userId, request);
        return APIResponse.onSuccess(SuccessCode.CREATED, resumeId);
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<APIResponse<ResumeDetailDto>> get(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId
    ) {
        Long userId = principal.userId();
        ResumeDetailDto data = resumeService.get(userId, resumeId);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }

    @GetMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<ResumeVersionDto>> getVersion(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @PathVariable int versionNo
    ) {
        Long userId = principal.userId();
        ResumeVersionDto data = resumeService.getVersion(userId, resumeId, versionNo);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }

    @PatchMapping("/{resumeId}/name")
    public ResponseEntity<APIResponse<Void>> rename(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @RequestBody ResumeRenameRequest request
    ) {
        Long userId = principal.userId();
        resumeService.rename(userId, resumeId, request);
        return APIResponse.onSuccess(SuccessCode.OK);
    }

    @PostMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<APIResponse<Void>> saveVersion(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId,
            @PathVariable int versionNo
    ) {
        Long userId = principal.userId();
        resumeService.saveVersion(userId, resumeId, versionNo);
        return APIResponse.onSuccess(SuccessCode.OK);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<APIResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long resumeId
    ) {
        Long userId = principal.userId();
        resumeService.delete(userId, resumeId);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}


//이력서 목록 조회
//이력서명 수정
//이력서 개별 조회
//이력서 특정 버전 조회
//이력서 생성
//이력서 수정
//이력서 수정 취소
//이력서 저장
//이력서 삭제
//리포지토리 목록 조회
//ai 작업 완료 콜백

