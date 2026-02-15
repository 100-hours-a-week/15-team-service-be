package com.sipomeokjo.commitme.domain.chat.pubsub;

import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;

public record ChatBroadcastPayload(Long chatroomId, ChatMessageResponse message) {}
