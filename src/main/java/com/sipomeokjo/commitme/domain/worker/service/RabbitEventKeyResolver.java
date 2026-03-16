package com.sipomeokjo.commitme.domain.worker.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Component
public class RabbitEventKeyResolver {

    public String resolve(String idempotencyKey, String eventId, Message message) {
        String candidate = firstNonBlank(idempotencyKey, eventId);
        if (candidate != null) {
            return normalize(candidate);
        }

        String messageId = message.getMessageProperties().getMessageId();
        if (messageId != null && !messageId.isBlank()) {
            return normalize(messageId);
        }

        return sha256(message.getBody());
    }

    private String normalize(String eventKey) {
        String normalized = eventKey.trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String sha256(byte[] bytes) {
        byte[] source = bytes == null ? new byte[0] : bytes;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source);
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return new String(source, StandardCharsets.UTF_8);
        }
    }
}
