package com.sipomeokjo.commitme.domain.chat.controller;

import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendRequest;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatBroadcastPayload;
import com.sipomeokjo.commitme.domain.chat.pubsub.ChatMessagePublisher;
import com.sipomeokjo.commitme.domain.chat.service.ChatMessageCommandService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageCommandService chatMessageCommandService;
    private final ChatMessagePublisher chatMessagePublisher;

    @MessageMapping("/chats/{chatroomId}")
    public void sendMessage(
            @DestinationVariable Long chatroomId,
            ChatMessageSendRequest request,
            Principal principal) {
        Long userId = resolveUserId(principal);
        ChatMessageResponse message =
                chatMessageCommandService.sendMessage(chatroomId, userId, request);
        chatMessagePublisher.publish(new ChatBroadcastPayload(chatroomId, message));
    }

    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        return Long.valueOf(principal.getName());
    }
}
