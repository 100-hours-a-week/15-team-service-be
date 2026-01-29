package com.sipomeokjo.commitme.domain.chat.dto;

import java.util.List;

public record ChatMessageSendRequest(
        String message, List<Long> attachmentUploadIds, Long mentionUserId) {}
