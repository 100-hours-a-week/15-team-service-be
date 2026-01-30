package com.sipomeokjo.commitme.domain.upload.dto;

import java.time.Instant;

public record UploadCreateResponse(
        Long uploadId, String presignedUrl, String s3Key, Instant expiresAt) {}
