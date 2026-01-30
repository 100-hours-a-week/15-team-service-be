package com.sipomeokjo.commitme.domain.upload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadConfirmRequest(@NotBlank String etag, @NotNull @Min(1) Long fileSize) {}
