package com.sipomeokjo.commitme.domain.chat.mapper;

import com.sipomeokjo.commitme.domain.chat.dto.ChatAttachmentResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachment;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageMapper {

    private final S3UploadService s3UploadService;

    public ChatMessageResponse toChatMessageResponse(
            ChatMessage message,
            Map<Long, List<ChatAttachment>> attachmentsByMessageId,
            Map<Long, Integer> userNumbersByUserId) {
        if (message == null) {
            return null;
        }

        List<ChatAttachmentResponse> files = null;
        if (attachmentsByMessageId != null) {
            List<ChatAttachment> attachments = attachmentsByMessageId.get(message.getId());
            if (attachments != null && !attachments.isEmpty()) {
                files =
                        attachments.stream()
                                .map(this::toChatAttachmentResponse)
                                .collect(Collectors.toList());
            }
        }

        Long mentionTo = message.getMentionUser() == null ? null : message.getMentionUser().getId();
        Long senderNumber = toNumber(userNumbersByUserId, message.getSender().getId());
        Long mentionToNumber = mentionTo == null ? null : toNumber(userNumbersByUserId, mentionTo);

        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getMessage(),
                files,
                message.getStatus(),
                message.getSender().getId(),
                senderNumber,
                mentionTo,
                mentionToNumber,
                message.getCreatedAt());
    }

    private ChatAttachmentResponse toChatAttachmentResponse(ChatAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        String fileUrl = s3UploadService.toPresignedGetUrl(attachment.getFileUrl());
        return new ChatAttachmentResponse(
                attachment.getId(), fileUrl, attachment.getFileType().name());
    }

    private Long toNumber(Map<Long, Integer> userNumbersByUserId, Long userId) {
        if (userNumbersByUserId == null || userId == null) {
            return null;
        }
        Integer number = userNumbersByUserId.get(userId);
        return number == null ? null : number.longValue();
    }
}
