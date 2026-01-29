package com.sipomeokjo.commitme.websocket;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Object userId = attributes.get("userId");
        if (userId == null) {
            return super.determineUser(request, wsHandler, attributes);
        }
        return new StompUserPrincipal(String.valueOf(userId));
    }

    private record StompUserPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
