package com.sipomeokjo.commitme.domain.chat.document;

import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachmentType;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatAttachmentEmbedded {

    private Long legacyId;
    private ChatAttachmentType fileType;
    private String fileUrl;
    private Integer orderNo;
    private Instant createdAt;

    @Builder
    public ChatAttachmentEmbedded(
            Long legacyId,
            ChatAttachmentType fileType,
            String fileUrl,
            Integer orderNo,
            Instant createdAt) {
        this.legacyId = legacyId;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
        this.orderNo = orderNo;
        this.createdAt = createdAt;
    }
}
