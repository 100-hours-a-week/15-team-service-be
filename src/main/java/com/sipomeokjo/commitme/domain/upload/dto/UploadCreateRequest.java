package com.sipomeokjo.commitme.domain.upload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadCreateRequest(
        @NotBlank String purpose,
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Min(1) Long fileSize) {}
