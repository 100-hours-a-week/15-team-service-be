package com.sipomeokjo.commitme.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitConfig {

    private final RabbitProperties rabbitProperties;

    @Bean
    public Declarables rabbitTopology() {
        TopicExchange exchange = new TopicExchange(rabbitProperties.getExchange(), true, false);

        Queue mainQueue =
                QueueBuilder.durable(rabbitProperties.getQueue())
                        .deadLetterExchange(rabbitProperties.getExchange())
                        .deadLetterRoutingKey(rabbitProperties.getDlqRoutingKey())
                        .build();

        Queue retryQueue =
                QueueBuilder.durable(rabbitProperties.getRetryQueue())
                        .ttl(rabbitProperties.getRetryTtlMs())
                        .deadLetterExchange(rabbitProperties.getExchange())
                        .deadLetterRoutingKey(rabbitProperties.getRoutingKey())
                        .build();

        Queue dlqQueue = QueueBuilder.durable(rabbitProperties.getDlqQueue()).build();

        Binding mainBinding =
                BindingBuilder.bind(mainQueue).to(exchange).with(rabbitProperties.getRoutingKey());
        Binding retryBinding =
                BindingBuilder.bind(retryQueue)
                        .to(exchange)
                        .with(rabbitProperties.getRetryRoutingKey());
        Binding dlqBinding =
                BindingBuilder.bind(dlqQueue)
                        .to(exchange)
                        .with(rabbitProperties.getDlqRoutingKey());

        return new Declarables(
                exchange, mainQueue, retryQueue, dlqQueue, mainBinding, retryBinding, dlqBinding);
    }
}
