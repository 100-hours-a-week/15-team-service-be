package com.sipomeokjo.commitme.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class WebSocketStompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable ex) {
        ErrorCode errorCode = resolveErrorCode(ex);
        byte[] payload = toPayload(errorCode);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorCode.getMessage());
        accessor.setContentType(MediaType.APPLICATION_JSON);
        accessor.setSessionId(getSessionId(clientMessage));

        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(payload, headers);
    }

    private ErrorCode resolveErrorCode(Throwable ex) {
        if (ex instanceof BusinessException businessException) {
            return businessException.getErrorCode();
        }
        if (ex instanceof AccessDeniedException) {
            return ErrorCode.FORBIDDEN;
        }
        if (ex instanceof AuthenticationException) {
            return ErrorCode.UNAUTHORIZED;
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    private byte[] toPayload(ErrorCode errorCode) {
        try {
            APIResponse<Void> body = APIResponse.body(errorCode);
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            log.warn("[WebSocket][ErrorHandler] 직렬화 실패 사유={}", e.getMessage());
            return new byte[0];
        }
    }

    private String getSessionId(Message<byte[]> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        return accessor.getSessionId();
    }
}
