package com.sipomeokjo.commitme.domain.chat.dto;

import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        ChatMessageRole role,
        String message,
        List<ChatAttachmentResponse> files,
        ChatMessageStatus status,
        Long sender,
        Long senderNumber,
        Long mentionTo,
        Long mentionToNumber,
        Instant sendAt) {}
