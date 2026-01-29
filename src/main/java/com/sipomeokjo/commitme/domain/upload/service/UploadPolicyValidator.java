package com.sipomeokjo.commitme.domain.upload.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.upload.entity.UploadPurpose;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UploadPolicyValidator {

    private static final long FIVE_MB = 5L * 1024L * 1024L;
    private static final long FIFTY_MB = 50L * 1024L * 1024L;

    private static final Set<String> IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private static final Set<String> AUDIO_CONTENT_TYPES = Set.of("audio/mpeg", "audio/wav");

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav");

    private static final Map<UploadPurpose, UploadPolicy> POLICIES =
            Map.of(
                    UploadPurpose.CHAT_ATTACHMENT,
                            new UploadPolicy(IMAGE_CONTENT_TYPES, IMAGE_EXTENSIONS, FIVE_MB),
                    UploadPurpose.PROFILE_IMAGE,
                            new UploadPolicy(IMAGE_CONTENT_TYPES, IMAGE_EXTENSIONS, FIVE_MB),
                    UploadPurpose.INTERVIEW_AUDIO,
                            new UploadPolicy(AUDIO_CONTENT_TYPES, AUDIO_EXTENSIONS, FIFTY_MB));

    public UploadPurpose parsePurpose(String purpose) {
        try {
            return UploadPurpose.valueOf(purpose);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_PURPOSE_INVALID);
        }
    }

    public void validateFilePolicy(
            UploadPurpose purpose, String fileName, String contentType, Long fileSize) {
        UploadPolicy policy = POLICIES.get(purpose);
        if (policy == null) {
            throw new BusinessException(ErrorCode.UPLOAD_PURPOSE_INVALID);
        }

        if (contentType == null || !policy.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.UPLOAD_CONTENT_TYPE_NOT_ALLOWED);
        }

        String extension = extractExtension(fileName);
        if (!policy.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.UPLOAD_EXTENSION_NOT_ALLOWED);
        }

        if (fileSize == null || fileSize > policy.maxSizeBytes()) {
            throw new BusinessException(ErrorCode.UPLOAD_FILE_SIZE_EXCEEDED);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.UPLOAD_EXTENSION_NOT_ALLOWED);
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.UPLOAD_EXTENSION_NOT_ALLOWED);
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private record UploadPolicy(
            Set<String> allowedContentTypes, Set<String> allowedExtensions, long maxSizeBytes) {}
}
