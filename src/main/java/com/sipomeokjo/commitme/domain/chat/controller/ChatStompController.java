package com.sipomeokjo.commitme.domain.chat.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendRequest;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendResponse;
import com.sipomeokjo.commitme.domain.chat.service.ChatMessageCommandService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageCommandService chatMessageCommandService;

    @MessageMapping("/chats/{chatroomId}")
    @SendTo("/topic/chats/{chatroomId}")
    public APIResponse<ChatMessageSendResponse> sendMessage(
            @DestinationVariable Long chatroomId,
            ChatMessageSendRequest request,
            Principal principal) {
        Long userId = resolveUserId(principal);
        ChatMessageResponse message =
                chatMessageCommandService.sendMessage(chatroomId, userId, request);
        return APIResponse.body(
                SuccessCode.CHAT_MESSAGE_SENT, new ChatMessageSendResponse(message));
    }

    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        return Long.valueOf(principal.getName());
    }
}
