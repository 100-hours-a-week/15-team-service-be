package com.sipomeokjo.commitme.domain.chat.entity;

import com.sipomeokjo.commitme.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "position_chat_message")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private Chatroom chatroom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mention_user_id")
    private User mentionUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChatMessageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private ChatMessageType messageType;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public ChatMessage(
            Long id,
            Chatroom chatroom,
            User sender,
            User mentionUser,
            ChatMessageStatus status,
            ChatMessageRole role,
            ChatMessageType messageType,
            String message,
            Instant createdAt) {
        this.id = id;
        this.chatroom = chatroom;
        this.sender = sender;
        this.mentionUser = mentionUser;
        this.status = status;
        this.role = role;
        this.messageType = messageType;
        this.message = message;
        this.createdAt = createdAt;
    }
}
