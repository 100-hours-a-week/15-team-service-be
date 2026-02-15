package com.sipomeokjo.commitme.domain.chat.pubsub;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final RedisTemplate<String, ChatBroadcastPayload> chatMessageRedisTemplate;
    private final ChannelTopic chatMessageTopic;

    public void publish(ChatBroadcastPayload payload) {
        chatMessageRedisTemplate.convertAndSend(chatMessageTopic.getTopic(), payload);
    }
}
