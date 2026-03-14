package com.sipomeokjo.commitme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.rabbit")
public class RabbitProperties {

    private String exchange = "commitme.events";
    private String queue = "commitme.events.queue";
    private String routingKey = "commitme.events";

    private String retryQueue = "commitme.events.queue.retry";
    private String retryRoutingKey = "commitme.events.retry";
    private int retryTtlMs = 5000;

    private String dlqQueue = "commitme.events.queue.dlq";
    private String dlqRoutingKey = "commitme.events.dlq";
}
