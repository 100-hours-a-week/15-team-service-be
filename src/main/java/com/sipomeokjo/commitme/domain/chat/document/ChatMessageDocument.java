package com.sipomeokjo.commitme.domain.chat.document;

import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@NoArgsConstructor
@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_chatroom_cursor", def = "{'chatroomId': 1, 'createdAt': -1, '_id': -1}")
public class ChatMessageDocument {

    @Id private String id;

    private Long chatroomId;
    private Long senderId;
    private Long mentionUserId;
    private ChatMessageStatus status;
    private ChatMessageRole role;
    private ChatMessageType messageType;
    private String message;
    private List<ChatAttachmentEmbedded> attachments = new ArrayList<>();
    private Instant createdAt;
    private Long legacyId;

    @Builder
    public ChatMessageDocument(
            String id,
            Long chatroomId,
            Long senderId,
            Long mentionUserId,
            ChatMessageStatus status,
            ChatMessageRole role,
            ChatMessageType messageType,
            String message,
            List<ChatAttachmentEmbedded> attachments,
            Instant createdAt,
            Long legacyId) {
        this.id = id;
        this.chatroomId = chatroomId;
        this.senderId = senderId;
        this.mentionUserId = mentionUserId;
        this.status = status;
        this.role = role;
        this.messageType = messageType;
        this.message = message;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
        this.createdAt = createdAt;
        this.legacyId = legacyId;
    }
}
