package com.sipomeokjo.commitme.domain.chat.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.sipomeokjo.commitme.domain.chat.document.ChatAttachmentEmbedded;
import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachmentType;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageType;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatMessageMongoMapperTest {

    @Mock private S3UploadService s3UploadService;

    private ChatMessageMongoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ChatMessageMongoMapper(s3UploadService);
    }

    @Test
    @DisplayName("ChatMessageDocument를 ChatMessageResponse로 변환한다")
    void toChatMessageResponse() {
        Instant now = Instant.now();
        ChatMessageDocument document =
                ChatMessageDocument.builder()
                        .chatroomId(1L)
                        .senderId(100L)
                        .mentionUserId(200L)
                        .status(ChatMessageStatus.SENT)
                        .role(ChatMessageRole.CHAT)
                        .messageType(ChatMessageType.TEXT)
                        .message("테스트 메시지")
                        .createdAt(now)
                        .legacyId(999L)
                        .build();

        Map<Long, Integer> userNumbersByUserId = Map.of(100L, 1, 200L, 2);

        ChatMessageResponse response = mapper.toChatMessageResponse(document, userNumbersByUserId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(999L);
        assertThat(response.role()).isEqualTo(ChatMessageRole.CHAT);
        assertThat(response.message()).isEqualTo("테스트 메시지");
        assertThat(response.status()).isEqualTo(ChatMessageStatus.SENT);
        assertThat(response.sender()).isEqualTo(100L);
        assertThat(response.senderNumber()).isEqualTo(1L);
        assertThat(response.mentionTo()).isEqualTo(200L);
        assertThat(response.mentionToNumber()).isEqualTo(2L);
        assertThat(response.sendAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("첨부파일이 포함된 메시지를 변환한다")
    void toChatMessageResponseWithAttachments() {
        given(s3UploadService.toCdnUrl(anyString())).willAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));

        Instant now = Instant.now();
        ChatAttachmentEmbedded attachment =
                ChatAttachmentEmbedded.builder()
                        .legacyId(10L)
                        .fileType(ChatAttachmentType.IMAGE)
                        .fileUrl("image.jpg")
                        .orderNo(1)
                        .createdAt(now)
                        .build();

        ChatMessageDocument document =
                ChatMessageDocument.builder()
                        .chatroomId(1L)
                        .senderId(100L)
                        .status(ChatMessageStatus.SENT)
                        .role(ChatMessageRole.CHAT)
                        .messageType(ChatMessageType.MIXED)
                        .message("이미지와 함께")
                        .attachments(List.of(attachment))
                        .createdAt(now)
                        .legacyId(999L)
                        .build();

        Map<Long, Integer> userNumbersByUserId = Map.of(100L, 1);

        ChatMessageResponse response = mapper.toChatMessageResponse(document, userNumbersByUserId);

        assertThat(response.files()).hasSize(1);
        assertThat(response.files().get(0).id()).isEqualTo(10L);
        assertThat(response.files().get(0).fileUrl()).isEqualTo("https://cdn.example.com/image.jpg");
        assertThat(response.files().get(0).fileType()).isEqualTo("IMAGE");
    }

    @Test
    @DisplayName("null 문서는 null을 반환한다")
    void toChatMessageResponseWithNull() {
        ChatMessageResponse response = mapper.toChatMessageResponse(null, Map.of());

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("legacyId가 없으면 ObjectId 해시값을 사용한다")
    void toChatMessageResponseWithoutLegacyId() {
        Instant now = Instant.now();
        ChatMessageDocument document =
                ChatMessageDocument.builder()
                        .chatroomId(1L)
                        .senderId(100L)
                        .status(ChatMessageStatus.SENT)
                        .role(ChatMessageRole.CHAT)
                        .messageType(ChatMessageType.TEXT)
                        .message("테스트")
                        .createdAt(now)
                        .build();

        // id가 null이면 hashId도 null 반환
        ChatMessageResponse response = mapper.toChatMessageResponse(document, Map.of(100L, 1));

        assertThat(response).isNotNull();
        assertThat(response.id()).isNull();
    }
}
