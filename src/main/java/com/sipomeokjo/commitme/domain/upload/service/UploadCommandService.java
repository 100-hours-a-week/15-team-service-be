package com.sipomeokjo.commitme.domain.upload.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.S3Properties;
import com.sipomeokjo.commitme.domain.upload.dto.UploadConfirmRequest;
import com.sipomeokjo.commitme.domain.upload.dto.UploadConfirmResponse;
import com.sipomeokjo.commitme.domain.upload.dto.UploadCreateRequest;
import com.sipomeokjo.commitme.domain.upload.dto.UploadCreateResponse;
import com.sipomeokjo.commitme.domain.upload.entity.Upload;
import com.sipomeokjo.commitme.domain.upload.entity.UploadPurpose;
import com.sipomeokjo.commitme.domain.upload.entity.UploadStatus;
import com.sipomeokjo.commitme.domain.upload.mapper.UploadMapper;
import com.sipomeokjo.commitme.domain.upload.repository.UploadRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UploadCommandService {

    private final UploadRepository uploadRepository;
    private final UploadPolicyValidator uploadPolicyValidator;
    private final UploadKeyGenerator uploadKeyGenerator;
    private final S3UploadService s3UploadService;
    private final UploadMapper uploadMapper;
    private final S3Properties s3Properties;
    private final Clock clock;

    public UploadCreateResponse createUpload(Long userId, UploadCreateRequest request) {
        UploadPurpose purpose = uploadPolicyValidator.parsePurpose(request.purpose());
        uploadPolicyValidator.validateFilePolicy(
                purpose, request.fileName(), request.contentType(), request.fileSize());

        String s3Key = uploadKeyGenerator.generate(purpose, request.fileName());
        Instant now = Instant.now(clock);
        Duration duration = Duration.ofMinutes(s3Properties.presignDurationMinutes());
        Instant expiresAt = now.plus(duration);

        Upload upload =
                Upload.builder()
                        .ownerUserId(userId)
                        .purpose(purpose)
                        .status(UploadStatus.PENDING)
                        .s3Key(s3Key)
                        .fileName(request.fileName())
                        .contentType(request.contentType())
                        .fileSize(request.fileSize())
                        .expiresAt(expiresAt)
                        .build();

        Upload saved = uploadRepository.save(upload);
        S3UploadService.PresignResult presignResult =
                s3UploadService.presignPutObject(s3Key, request.contentType());
        return uploadMapper.toUploadCreateResponse(saved, presignResult.presignedUrl());
    }

    public UploadConfirmResponse confirmUpload(
            Long userId, Long uploadId, UploadConfirmRequest request) {
        Upload upload =
                uploadRepository
                        .findById(uploadId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UPLOAD_NOT_FOUND));

        if (!upload.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.UPLOAD_FORBIDDEN);
        }

        Instant now = Instant.now(clock);
        upload.markExpired(now);
        if (upload.getStatus() == UploadStatus.EXPIRED) {
            uploadRepository.save(upload);
            throw new BusinessException(ErrorCode.UPLOAD_EXPIRED);
        }

        if (upload.getStatus() != UploadStatus.PENDING) {
            throw new BusinessException(ErrorCode.UPLOAD_STATUS_INVALID);
        }

        if (!upload.getFileSize().equals(request.fileSize())) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_SIZE_MISMATCH);
        }

        S3UploadService.HeadResult headResult = s3UploadService.headObject(upload.getS3Key());
        if (headResult.contentLength() != upload.getFileSize()) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_SIZE_MISMATCH);
        }

        String requestEtag = normalizeEtag(request.etag());
        String headEtag = normalizeEtag(headResult.eTag());
        if (!requestEtag.equals(headEtag)) {
            throw new BusinessException(ErrorCode.UPLOAD_ETAG_MISMATCH);
        }

        upload.confirmUploaded(headResult.eTag(), now);
        uploadRepository.save(upload);
        return uploadMapper.toUploadConfirmResponse(upload);
    }

    private String normalizeEtag(String etag) {
        if (etag == null) {
            return "";
        }
        String trimmed = etag.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
