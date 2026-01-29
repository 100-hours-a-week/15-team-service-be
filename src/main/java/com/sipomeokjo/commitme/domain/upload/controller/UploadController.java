package com.sipomeokjo.commitme.domain.upload.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.upload.dto.UploadConfirmRequest;
import com.sipomeokjo.commitme.domain.upload.dto.UploadConfirmResponse;
import com.sipomeokjo.commitme.domain.upload.dto.UploadCreateRequest;
import com.sipomeokjo.commitme.domain.upload.dto.UploadCreateResponse;
import com.sipomeokjo.commitme.domain.upload.service.UploadCommandService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final UploadCommandService uploadCommandService;

    @PostMapping("/uploads")
    public ResponseEntity<APIResponse<UploadCreateResponse>> createUpload(
            @CurrentUserId Long userId, @Valid @RequestBody UploadCreateRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.UPLOAD_URL_ISSUED, uploadCommandService.createUpload(userId, request));
    }

    @PatchMapping("/uploads/{uploadId}")
    public ResponseEntity<APIResponse<UploadConfirmResponse>> confirmUpload(
            @CurrentUserId Long userId,
            @PathVariable Long uploadId,
            @Valid @RequestBody UploadConfirmRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.UPLOAD_CONFIRMED,
                uploadCommandService.confirmUpload(userId, uploadId, request));
    }
}
