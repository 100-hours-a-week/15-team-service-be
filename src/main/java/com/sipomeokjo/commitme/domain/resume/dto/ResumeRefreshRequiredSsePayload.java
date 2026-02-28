package com.sipomeokjo.commitme.domain.resume.dto;

public record ResumeRefreshRequiredSsePayload(Long resumeId, Integer versionNo, String status) {}
