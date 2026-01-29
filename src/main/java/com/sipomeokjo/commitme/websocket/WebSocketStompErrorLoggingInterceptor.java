package com.sipomeokjo.commitme.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketStompErrorLoggingInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.ERROR.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            String userName = accessor.getUser() == null ? null : accessor.getUser().getName();
            String errorMessage = accessor.getMessage();

            log.warn(
                    "[WebSocket][StompError] sessionId={} userId={} 사유={}",
                    sessionId,
                    userName,
                    errorMessage == null ? "unknown" : errorMessage);
        }
        return message;
    }
}
