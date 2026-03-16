package com.sipomeokjo.commitme.domain.worker.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

class RabbitEventKeyResolverTest {
    private final RabbitEventKeyResolver rabbitEventKeyResolver = new RabbitEventKeyResolver();

    @Test
    void resolve_prefersTrimmedIdempotencyKey() {
        Message message = MessageBuilder.withBody("body".getBytes(StandardCharsets.UTF_8)).build();

        String eventKey = rabbitEventKeyResolver.resolve("  idem-key  ", "event-id", message);

        assertThat(eventKey).isEqualTo("idem-key");
    }

    @Test
    void resolve_usesMessageIdWhenIdempotencyKeyIsMissing() {
        Message message =
                MessageBuilder.withBody("body".getBytes(StandardCharsets.UTF_8))
                        .setMessageId("message-id")
                        .build();

        String eventKey = rabbitEventKeyResolver.resolve(null, null, message);

        assertThat(eventKey).isEqualTo("message-id");
    }

    @Test
    void resolve_usesBodyHashWhenIdempotencyKeyAndMessageIdAreMissing() {
        Message message = MessageBuilder.withBody("body".getBytes(StandardCharsets.UTF_8)).build();

        String eventKey = rabbitEventKeyResolver.resolve(null, null, message);

        assertThat(eventKey)
                .isEqualTo("230d8358dc8e8890b4c58deeb62912ee2f20357ae92a5cc861b98e68fe31acb5");
    }
}
