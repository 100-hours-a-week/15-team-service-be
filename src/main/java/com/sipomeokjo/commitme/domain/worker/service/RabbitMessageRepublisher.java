package com.sipomeokjo.commitme.domain.worker.service;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMessageRepublisher {
    private final RabbitTemplate rabbitTemplate;

    public void republishJsonBody(Message sourceMessage, String exchange, String routingKey) {
        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                new String(sourceMessage.getBody(), StandardCharsets.UTF_8),
                postMessage -> {
                    if (sourceMessage.getMessageProperties().getMessageId() != null) {
                        postMessage
                                .getMessageProperties()
                                .setMessageId(sourceMessage.getMessageProperties().getMessageId());
                    }
                    postMessage.getMessageProperties().setContentType("application/json");
                    return postMessage;
                });
    }

    public void send(Message message, String exchange, String routingKey) {
        rabbitTemplate.send(exchange, routingKey, message);
    }
}
