package com.sipomeokjo.commitme.websocket;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final AccessTokenProvider accessTokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        HttpServletResponse httpResponse = servletResponse.getServletResponse();

        String accessToken = extractCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        if (!accessTokenProvider.validateToken(accessToken)) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        String status = accessTokenProvider.getStatus(accessToken);
        if (!UserStatus.ACTIVE.name().equals(status)) {
            httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        Long userId;
        try {
            userId = accessTokenProvider.getUserId(accessToken);
        } catch (RuntimeException e) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        if (userId == null) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        if (exception != null) {
            log.warn("[WebSocket][Handshake] 실패 사유={}", exception.getMessage());
            return;
        }

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String accessToken = extractCookie(httpRequest, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("[WebSocket][Handshake] 성공");
            return;
        }

        try {
            Long userId = accessTokenProvider.getUserId(accessToken);
            log.info("[WebSocket][Handshake] 성공 userId={}", userId);
        } catch (RuntimeException e) {
            log.warn("[WebSocket][Handshake] 성공(사용자 식별 실패) 사유={}", e.getMessage());
        }
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
