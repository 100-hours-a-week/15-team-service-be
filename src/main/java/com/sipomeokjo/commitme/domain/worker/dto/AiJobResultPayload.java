package com.sipomeokjo.commitme.domain.worker.dto;

public record AiJobResultPayload(
        Long userId,
        String notificationPayloadJson,
        String source,
        Long resumeId,
        Integer versionNo,
        String status) {}
