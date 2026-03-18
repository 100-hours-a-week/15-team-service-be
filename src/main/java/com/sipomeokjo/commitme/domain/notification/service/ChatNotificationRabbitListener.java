package com.sipomeokjo.commitme.domain.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sipomeokjo.commitme.config.RabbitProperties;
import com.sipomeokjo.commitme.domain.notification.dto.ChatEventEnvelope;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationRequestedPayload;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.worker.service.EventConsumeIdempotencyService;
import com.sipomeokjo.commitme.domain.worker.service.EventConsumeStartResult;
import com.sipomeokjo.commitme.domain.worker.service.RabbitEventKeyResolver;
import com.sipomeokjo.commitme.domain.worker.service.RabbitMessageRepublisher;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationRabbitListener {
    private static final String WORKER = "chat-notification-worker";
    private static final String RETRY_COUNT_HEADER = "x-retry-count";
    private static final TypeReference<ChatEventEnvelope<NotificationRequestedPayload>>
            NOTIFICATION_REQUESTED_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EventConsumeIdempotencyService eventConsumeIdempotencyService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseDispatchService notificationSseDispatchService;
    private final RabbitEventKeyResolver rabbitEventKeyResolver;
    private final RabbitMessageRepublisher rabbitMessageRepublisher;
    private final RabbitProperties rabbitProperties;
    private final MeterRegistry meterRegistry;

    @RabbitListener(
            queues = "${app.rabbit.notification.queue}",
            containerFactory = "workerManualAckRabbitListenerContainerFactory")
    public void consume(
            Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        String queueName = message.getMessageProperties().getConsumerQueue();
        String eventKey = null;

        try {
            ChatEventEnvelope<NotificationRequestedPayload> event =
                    objectMapper.readValue(message.getBody(), NOTIFICATION_REQUESTED_TYPE);

            if (event == null || event.payload() == null) {
                incrementConsume("invalid");
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (!"NotificationRequested".equalsIgnoreCase(event.eventType())) {
                incrementConsume("skipped");
                channel.basicAck(deliveryTag, false);
                return;
            }

            eventKey =
                    rabbitEventKeyResolver.resolve(
                            event.idempotencyKey(), event.eventId(), message);
            EventConsumeStartResult startResult =
                    eventConsumeIdempotencyService.tryStart(WORKER, eventKey, queueName);
            if (startResult == EventConsumeStartResult.ALREADY_SUCCEEDED) {
                incrementConsume("duplicate");
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (startResult == EventConsumeStartResult.IN_PROGRESS) {
                incrementConsume("in_progress");
                if (publishRetryOrDlq(message, "in_progress")) {
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                channel.basicNack(deliveryTag, false, true);
                return;
            }

            NotificationRequestedPayload payload = event.payload();
            if (!isProcessable(payload)) {
                eventConsumeIdempotencyService.markSuccess(WORKER, eventKey);
                incrementConsume("invalid");
                channel.basicAck(deliveryTag, false);
                return;
            }

            User user = userRepository.findById(payload.targetUserId()).orElse(null);
            if (user == null) {
                eventConsumeIdempotencyService.markSuccess(WORKER, eventKey);
                incrementConsume("skipped");
                channel.basicAck(deliveryTag, false);
                return;
            }

            Notification saved =
                    notificationRepository.save(
                            Notification.create(
                                    user, NotificationType.CHAT, toPayloadJson(payload)));
            notificationSseDispatchService.dispatchAsync(saved.getId());

            eventConsumeIdempotencyService.markSuccess(WORKER, eventKey);
            incrementConsume("success");
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            if (eventKey != null) {
                eventConsumeIdempotencyService.releaseOnFailure(WORKER, eventKey);
            }
            if (publishRetryOrDlq(message, ex.getClass().getSimpleName())) {
                incrementConsume("retry");
                channel.basicAck(deliveryTag, false);
                return;
            }
            incrementConsume("failed");
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private boolean publishRetryOrDlq(Message message, String reason) {
        RabbitProperties.Notification notification = rabbitProperties.getNotification();
        int retryCount = getRetryCount(message) + 1;
        String routingKey;
        String result;
        if (retryCount <= notification.getMaxRetryCount()) {
            routingKey = notification.getRetryRoutingKey();
            result = "retry";
        } else {
            routingKey = notification.getDlqRoutingKey();
            result = "dlq";
        }

        try {
            Message outbound =
                    MessageBuilder.fromMessage(message)
                            .setHeader(RETRY_COUNT_HEADER, retryCount)
                            .build();
            rabbitMessageRepublisher.send(outbound, notification.getExchange(), routingKey);
            meterRegistry
                    .counter("chat_notification_retry_total", "result", result, "reason", reason)
                    .increment();
            return true;
        } catch (Exception retryEx) {
            meterRegistry
                    .counter("chat_notification_retry_total", "result", "publish_failed")
                    .increment();
            log.warn(
                    "[CHAT_NOTIFICATION_WORKER] retry_publish_failed result={} reason={}",
                    result,
                    reason,
                    retryEx);
            return false;
        }
    }

    private int getRetryCount(Message message) {
        Object retryCountHeader =
                message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
        if (retryCountHeader instanceof Number number) {
            return number.intValue();
        }
        if (retryCountHeader instanceof String value) {
            try {
                return Integer.parseInt(value);
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isProcessable(NotificationRequestedPayload payload) {
        if (payload == null
                || payload.targetUserId() == null
                || payload.chatroomId() == null
                || payload.senderId() == null
                || payload.messageId() == null
                || payload.messageId().isBlank()) {
            return false;
        }
        if (!"CHAT".equalsIgnoreCase(payload.notificationType())) {
            return false;
        }
        return !payload.targetUserId().equals(payload.senderId());
    }

    private String toPayloadJson(NotificationRequestedPayload payload) throws Exception {
        Map<String, Object> notificationPayload = new LinkedHashMap<>();
        notificationPayload.put("sourceEventId", payload.sourceEventId());
        notificationPayload.put("messageId", payload.messageId());
        notificationPayload.put("chatroomId", payload.chatroomId());
        notificationPayload.put("chatroomName", payload.chatroomName());
        notificationPayload.put("senderId", payload.senderId());
        notificationPayload.put("senderName", payload.senderName());
        notificationPayload.put(
                "createdAt", payload.createdAt() == null ? Instant.now() : payload.createdAt());
        notificationPayload.put("messagePreview", payload.messagePreview());
        return objectMapper.writeValueAsString(notificationPayload);
    }

    private void incrementConsume(String result) {
        meterRegistry.counter("chat_notification_consume_total", "result", result).increment();
    }
}
