package com.sipomeokjo.commitme.domain.chat.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSubscriber implements MessageListener {
    private static final String CHAT_TOPIC_PREFIX = "/topic/chats/";

    private final GenericJackson2JsonRedisSerializer chatMessageSerializer;

    @Qualifier("chatRedisObjectMapper")
    private final ObjectMapper chatRedisObjectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        ChatBroadcastPayload payload = deserialize(message);
        if (payload == null || payload.chatroomId() == null) {
            return;
        }
        APIResponse<ChatMessageSendResponse> response =
                APIResponse.body(
                        SuccessCode.CHAT_MESSAGE_SENT,
                        new ChatMessageSendResponse(payload.message()));
        messagingTemplate.convertAndSend(CHAT_TOPIC_PREFIX + payload.chatroomId(), response);
    }

    private ChatBroadcastPayload deserialize(Message message) {
        try {
            Object value = chatMessageSerializer.deserialize(message.getBody());
            if (value instanceof ChatBroadcastPayload payload) {
                return payload;
            }
            if (value != null) {
                return chatRedisObjectMapper.convertValue(value, ChatBroadcastPayload.class);
            }
            return null;
        } catch (Exception e) {
            log.warn("[Chat][PubSub] 메시지 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
