package com.sipomeokjo.commitme.domain.resume.controller;

import com.sipomeokjo.commitme.api.pagination.PageResponse;
import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeRenameRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDto;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    // ✅ v1: 인증/인가 아직이면 임시로 userId를 파라미터로 받거나 하드코딩해서 테스트
    // 나중에 인증 붙으면 userId 주입 방식으로 바꾸면 됨.
    private Long mockUserId() {
        return 1L;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ResumeSummaryDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<ResumeSummaryDto> data = resumeService.list(mockUserId(), page, size);
        return ApiResponse.onSuccess(SuccessCode.OK, data);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody ResumeCreateRequest request) {
        Long resumeId = resumeService.create(mockUserId(), request);
        return ApiResponse.onSuccess(SuccessCode.CREATED, resumeId);
    }

    @GetMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<ResumeDetailDto>> get(@PathVariable Long resumeId) {
        ResumeDetailDto data = resumeService.get(mockUserId(), resumeId);
        return ApiResponse.onSuccess(SuccessCode.OK, data);
    }

    @GetMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<ApiResponse<ResumeVersionDto>> getVersion(
            @PathVariable Long resumeId,
            @PathVariable int versionNo
    ) {
        ResumeVersionDto data = resumeService.getVersion(mockUserId(), resumeId, versionNo);
        return ApiResponse.onSuccess(SuccessCode.OK, data);
    }

    @PatchMapping("/{resumeId}/name")
    public ResponseEntity<ApiResponse<Void>> rename(
            @PathVariable Long resumeId,
            @RequestBody ResumeRenameRequest request
    ) {
        resumeService.rename(mockUserId(), resumeId, request);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @PostMapping("/{resumeId}/versions/{versionNo}")
    public ResponseEntity<ApiResponse<Void>> saveVersion(
            @PathVariable Long resumeId,
            @PathVariable int versionNo
    ) {
        resumeService.saveVersion(mockUserId(), resumeId, versionNo);
        return ApiResponse.onSuccess(SuccessCode.OK);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long resumeId) {
        resumeService.delete(mockUserId(), resumeId);
        return ApiResponse.onSuccess(SuccessCode.OK);
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

