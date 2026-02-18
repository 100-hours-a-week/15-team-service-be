package com.sipomeokjo.commitme.domain.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.chat.controller.ChatStompController;
import com.sipomeokjo.commitme.domain.chat.dto.ChatAttachmentResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendRequest;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatBroadcastPayload;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatMessagePublisher;
import com.sipomeokjo.commitme.domain.chat.service.ChatMessageCommandService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatStompControllerTest {

    @Mock private ChatMessageCommandService chatMessageCommandService;

    @Mock private ChatMessagePublisher chatMessagePublisher;

    @InjectMocks private ChatStompController chatStompController;

    @Test
    void sendMessage_publishesMessage() {
        ChatMessageResponse response =
                new ChatMessageResponse(
                        10L,
                        ChatMessageRole.CHAT,
                        "안녕하세요",
                        List.of(new ChatAttachmentResponse(1L, "file-url", "IMAGE")),
                        ChatMessageStatus.SENT,
                        1L,
                        1L,
                        null,
                        null,
                        Instant.parse("2026-01-30T06:20:00Z"));
        given(
                        chatMessageCommandService.sendMessage(
                                eq(1L), eq(1L), any(ChatMessageSendRequest.class)))
                .willReturn(response);

        ChatMessageSendRequest request = new ChatMessageSendRequest("안녕하세요", List.of(), null);
        Principal principal = () -> "1";

        chatStompController.sendMessage(1L, request, principal);

        verify(chatMessageCommandService).sendMessage(1L, 1L, request);
        verify(chatMessagePublisher).publish(new ChatBroadcastPayload(1L, response));
    }
}
