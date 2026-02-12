package com.sipomeokjo.commitme.domain.chat.mapper;

import com.sipomeokjo.commitme.domain.chat.document.ChatAttachmentEmbedded;
import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import com.sipomeokjo.commitme.domain.chat.dto.ChatAttachmentResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageMongoMapper {

    private final S3UploadService s3UploadService;

    public ChatMessageResponse toChatMessageResponse(
            ChatMessageDocument document, Map<Long, Integer> userNumbersByUserId) {
        if (document == null) {
            return null;
        }

        List<ChatAttachmentResponse> files = null;
        if (document.getAttachments() != null && !document.getAttachments().isEmpty()) {
            files =
                    document.getAttachments().stream()
                            .map(this::toChatAttachmentResponse)
                            .collect(Collectors.toList());
        }

        Long mentionTo = document.getMentionUserId();
        Long senderNumber = toNumber(userNumbersByUserId, document.getSenderId());
        Long mentionToNumber = mentionTo == null ? null : toNumber(userNumbersByUserId, mentionTo);

        return new ChatMessageResponse(
                document.getLegacyId() != null ? document.getLegacyId() : hashId(document.getId()),
                document.getRole(),
                document.getMessage(),
                files,
                document.getStatus(),
                document.getSenderId(),
                senderNumber,
                mentionTo,
                mentionToNumber,
                document.getCreatedAt());
    }

    private ChatAttachmentResponse toChatAttachmentResponse(ChatAttachmentEmbedded attachment) {
        if (attachment == null) {
            return null;
        }
        String fileUrl = s3UploadService.toCdnUrl(attachment.getFileUrl());
        return new ChatAttachmentResponse(
                attachment.getLegacyId() != null
                        ? attachment.getLegacyId()
                        : attachment.getOrderNo().longValue(),
                fileUrl,
                attachment.getFileType().name());
    }

    private Long toNumber(Map<Long, Integer> userNumbersByUserId, Long userId) {
        if (userNumbersByUserId == null || userId == null) {
            return null;
        }
        Integer number = userNumbersByUserId.get(userId);
        return number == null ? null : number.longValue();
    }

    private Long hashId(String objectId) {
        if (objectId == null) {
            return null;
        }
        return Math.abs(objectId.hashCode()) & 0xFFFFFFFFL;
    }
}
