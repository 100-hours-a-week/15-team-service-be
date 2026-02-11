package com.sipomeokjo.commitme.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sipomeokjo.commitme.domain.chat.document.ChatAttachmentEmbedded;
import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachmentType;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageType;
import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

@DataMongoTest
@ContextConfiguration(classes = MongoTestConfig.class)
@EnableMongoRepositories(basePackageClasses = ChatMessageMongoRepository.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=6.0.5"
})
class ChatMessageMongoRepositoryTest {

    @Autowired private ChatMessageMongoRepository chatMessageMongoRepository;

    private static final Long CHATROOM_ID = 1L;
    private static final Long SENDER_ID = 100L;

    @BeforeEach
    void setUp() {
        chatMessageMongoRepository.deleteAll();
    }

    @Test
    @DisplayName("채팅방 ID로 메시지 목록을 최신순으로 조회한다")
    void findByChatroomIdOrderByCreatedAtDesc() {
        Instant now = Instant.now();
        ChatMessageDocument message1 = createMessage(CHATROOM_ID, "첫번째 메시지", now.minusSeconds(100));
        ChatMessageDocument message2 = createMessage(CHATROOM_ID, "두번째 메시지", now.minusSeconds(50));
        ChatMessageDocument message3 = createMessage(CHATROOM_ID, "세번째 메시지", now);

        chatMessageMongoRepository.saveAll(List.of(message1, message2, message3));

        List<ChatMessageDocument> result =
                chatMessageMongoRepository.findByChatroomIdOrderByCreatedAtDesc(
                        CHATROOM_ID, PageRequest.of(0, 10));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getMessage()).isEqualTo("세번째 메시지");
        assertThat(result.get(1).getMessage()).isEqualTo("두번째 메시지");
        assertThat(result.get(2).getMessage()).isEqualTo("첫번째 메시지");
    }

    @Test
    @DisplayName("커서 기반으로 메시지 목록을 조회한다")
    void findByChatroomWithCursor() {
        Instant now = Instant.now();
        ChatMessageDocument message1 = createMessage(CHATROOM_ID, "첫번째 메시지", now.minusSeconds(100));
        ChatMessageDocument message2 = createMessage(CHATROOM_ID, "두번째 메시지", now.minusSeconds(50));
        ChatMessageDocument message3 = createMessage(CHATROOM_ID, "세번째 메시지", now);

        List<ChatMessageDocument> saved =
                chatMessageMongoRepository.saveAll(List.of(message1, message2, message3));

        ChatMessageDocument cursorMessage = saved.stream()
                .filter(m -> m.getMessage().equals("세번째 메시지"))
                .findFirst()
                .orElseThrow();

        List<ChatMessageDocument> result =
                chatMessageMongoRepository.findByChatroomWithCursor(
                        CHATROOM_ID,
                        cursorMessage.getCreatedAt(),
                        new ObjectId(cursorMessage.getId()),
                        PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMessage()).isEqualTo("두번째 메시지");
        assertThat(result.get(1).getMessage()).isEqualTo("첫번째 메시지");
    }

    @Test
    @DisplayName("채팅방별 최신 메시지를 조회한다")
    void findLatestByChatroomId() {
        Instant now = Instant.now();
        ChatMessageDocument message1 = createMessage(CHATROOM_ID, "이전 메시지", now.minusSeconds(100));
        ChatMessageDocument message2 = createMessage(CHATROOM_ID, "최신 메시지", now);

        chatMessageMongoRepository.saveAll(List.of(message1, message2));

        List<ChatMessageDocument> result =
                chatMessageMongoRepository.findLatestByChatroomId(CHATROOM_ID, PageRequest.of(0, 1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("최신 메시지");
    }

    @Test
    @DisplayName("채팅방별 메시지 수를 카운트한다")
    void countByChatroomId() {
        ChatMessageDocument message1 = createMessage(CHATROOM_ID, "메시지 1", Instant.now());
        ChatMessageDocument message2 = createMessage(CHATROOM_ID, "메시지 2", Instant.now());
        ChatMessageDocument message3 = createMessage(2L, "다른 채팅방 메시지", Instant.now());

        chatMessageMongoRepository.saveAll(List.of(message1, message2, message3));

        long count = chatMessageMongoRepository.countByChatroomId(CHATROOM_ID);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("첨부파일이 포함된 메시지를 저장하고 조회한다")
    void saveMessageWithAttachments() {
        ChatAttachmentEmbedded attachment1 =
                ChatAttachmentEmbedded.builder()
                        .fileType(ChatAttachmentType.IMAGE)
                        .fileUrl("https://example.com/image1.jpg")
                        .orderNo(1)
                        .createdAt(Instant.now())
                        .build();

        ChatAttachmentEmbedded attachment2 =
                ChatAttachmentEmbedded.builder()
                        .fileType(ChatAttachmentType.IMAGE)
                        .fileUrl("https://example.com/image2.jpg")
                        .orderNo(2)
                        .createdAt(Instant.now())
                        .build();

        ChatMessageDocument message =
                ChatMessageDocument.builder()
                        .chatroomId(CHATROOM_ID)
                        .senderId(SENDER_ID)
                        .status(ChatMessageStatus.SENT)
                        .role(ChatMessageRole.CHAT)
                        .messageType(ChatMessageType.MIXED)
                        .message("이미지와 함께 보내는 메시지")
                        .attachments(List.of(attachment1, attachment2))
                        .createdAt(Instant.now())
                        .build();

        ChatMessageDocument saved = chatMessageMongoRepository.save(message);

        ChatMessageDocument found = chatMessageMongoRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAttachments()).hasSize(2);
        assertThat(found.getAttachments().get(0).getFileUrl())
                .isEqualTo("https://example.com/image1.jpg");
        assertThat(found.getAttachments().get(1).getFileUrl())
                .isEqualTo("https://example.com/image2.jpg");
    }

    private ChatMessageDocument createMessage(Long chatroomId, String message, Instant createdAt) {
        return ChatMessageDocument.builder()
                .chatroomId(chatroomId)
                .senderId(SENDER_ID)
                .status(ChatMessageStatus.SENT)
                .role(ChatMessageRole.CHAT)
                .messageType(ChatMessageType.TEXT)
                .message(message)
                .createdAt(createdAt)
                .build();
    }
}
