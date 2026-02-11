package com.sipomeokjo.commitme.domain.chat.migration;

import com.sipomeokjo.commitme.domain.chat.document.ChatAttachmentEmbedded;
import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachment;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import com.sipomeokjo.commitme.domain.chat.repository.ChatAttachmentRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageMongoRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageMigrationService {

    private static final int BATCH_SIZE = 500;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatMessageMongoRepository chatMessageMongoRepository;

    @Transactional(readOnly = true)
    public MigrationResult migrate() {
        log.info("Starting chat message migration from MySQL to MongoDB");

        long totalMigrated = 0;
        long totalFailed = 0;
        int page = 0;

        Page<ChatMessage> messagePage;
        do {
            messagePage =
                    chatMessageRepository.findAll(
                            PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id")));

            List<ChatMessage> messages = messagePage.getContent();
            if (messages.isEmpty()) {
                break;
            }

            List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();

            Map<Long, List<ChatAttachment>> attachmentsByMessageId =
                    chatAttachmentRepository.findByMessageIdIn(messageIds).stream()
                            .collect(
                                    Collectors.groupingBy(
                                            attachment -> attachment.getMessage().getId()));

            List<ChatMessageDocument> documents = new ArrayList<>();
            for (ChatMessage message : messages) {
                try {
                    List<ChatAttachment> attachments =
                            attachmentsByMessageId.getOrDefault(message.getId(), List.of());
                    ChatMessageDocument document = toDocument(message, attachments);
                    documents.add(document);
                } catch (Exception e) {
                    log.error("Failed to convert message id={}: {}", message.getId(), e.getMessage());
                    totalFailed++;
                }
            }

            if (!documents.isEmpty()) {
                chatMessageMongoRepository.saveAll(documents);
                totalMigrated += documents.size();
                log.info(
                        "Migrated batch {}: {} messages (total: {})",
                        page,
                        documents.size(),
                        totalMigrated);
            }

            page++;
        } while (messagePage.hasNext());

        log.info(
                "Migration completed. Total migrated: {}, Failed: {}", totalMigrated, totalFailed);
        return new MigrationResult(totalMigrated, totalFailed);
    }

    private ChatMessageDocument toDocument(
            ChatMessage message, List<ChatAttachment> attachments) {
        List<ChatAttachmentEmbedded> embeddedAttachments =
                attachments.stream()
                        .map(
                                attachment ->
                                        ChatAttachmentEmbedded.builder()
                                                .legacyId(attachment.getId())
                                                .fileType(attachment.getFileType())
                                                .fileUrl(attachment.getFileUrl())
                                                .orderNo(attachment.getOrderNo())
                                                .createdAt(attachment.getCreatedAt())
                                                .build())
                        .toList();

        return ChatMessageDocument.builder()
                .chatroomId(message.getChatroom().getId())
                .senderId(message.getSender().getId())
                .mentionUserId(
                        message.getMentionUser() != null ? message.getMentionUser().getId() : null)
                .status(message.getStatus())
                .role(message.getRole())
                .messageType(message.getMessageType())
                .message(message.getMessage())
                .attachments(embeddedAttachments)
                .createdAt(message.getCreatedAt())
                .legacyId(message.getId())
                .build();
    }

    public record MigrationResult(long migratedCount, long failedCount) {}
}
