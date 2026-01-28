package com.sipomeokjo.commitme.domain.upload.dto;

import java.time.Instant;

public record UploadConfirmResponse(
        Long uploadId, Long attachmentId, String s3Key, Instant uploadedAt) {}
