package com.sipomeokjo.commitme.domain.chat.dto;

public record ChatAttachmentResponse(
        Long id,
        String fileUrl,
        String fileType
) {
}
