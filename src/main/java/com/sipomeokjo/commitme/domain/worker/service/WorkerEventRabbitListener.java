package com.sipomeokjo.commitme.domain.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sipomeokjo.commitme.config.RabbitProperties;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobRequestedPayload;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobResultPayload;
import com.sipomeokjo.commitme.domain.worker.dto.WorkerEventEnvelope;
import com.sipomeokjo.commitme.domain.worker.dto.WorkerEventType;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerEventRabbitListener {
    private static final String WORKER_AI_REQUESTED = "ai-job-requested-worker";
    private static final String WORKER_AI_RESULT = "ai-job-result-worker";

    private final ObjectMapper objectMapper;
    private final EventConsumeIdempotencyService eventConsumeIdempotencyService;
    private final AiJobRequestedWorker aiJobRequestedWorker;
    private final AiJobResultWorker aiJobResultWorker;
    private final RabbitEventKeyResolver rabbitEventKeyResolver;
    private final RabbitMessageRepublisher rabbitMessageRepublisher;
    private final RabbitProperties rabbitProperties;
    private final MeterRegistry meterRegistry;

    @RabbitListener(
            queues = "${app.rabbit.queue}",
            containerFactory = "workerManualAckRabbitListenerContainerFactory")
    public void consume(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String queueName = message.getMessageProperties().getConsumerQueue();

        WorkerEventEnvelope envelope;
        try {
            JsonNode root = objectMapper.readTree(message.getBody());
            envelope =
                    new WorkerEventEnvelope(
                            firstText(root, "eventId", "event_id"),
                            firstText(root, "eventType", "event_type", "type"),
                            firstText(root, "idempotencyKey", "idempotency_key"),
                            firstNode(root, "payload", "data"));
        } catch (Exception ex) {
            meterRegistry
                    .counter("worker_consume_total", "worker", "unknown", "result", "invalid")
                    .increment();
            log.warn("[WORKER_CONSUMER] invalid_message_payload queue={}", queueName, ex);
            channel.basicAck(deliveryTag, false);
            return;
        }

        WorkerEventType eventType = envelope.resolveType();
        if (eventType == null) {
            meterRegistry
                    .counter("worker_consume_total", "worker", "unknown", "result", "skipped")
                    .increment();
            log.debug(
                    "[WORKER_CONSUMER] unsupported_event_type queue={} eventType={}",
                    queueName,
                    envelope.eventType());
            channel.basicAck(deliveryTag, false);
            return;
        }

        String workerName = resolveWorkerName(eventType);
        String eventKey =
                rabbitEventKeyResolver.resolve(
                        envelope.idempotencyKey(), envelope.eventId(), message);
        EventConsumeStartResult startResult =
                eventConsumeIdempotencyService.tryStart(workerName, eventKey, queueName);
        if (startResult == EventConsumeStartResult.ALREADY_SUCCEEDED) {
            meterRegistry
                    .counter("worker_consume_total", "worker", workerName, "result", "duplicate")
                    .increment();
            channel.basicAck(deliveryTag, false);
            return;
        }
        if (startResult == EventConsumeStartResult.IN_PROGRESS) {
            meterRegistry
                    .counter("worker_consume_total", "worker", workerName, "result", "in_progress")
                    .increment();
            log.debug(
                    "[WORKER_CONSUMER] consume_deferred worker={} eventType={} eventKey={} queue={}",
                    workerName,
                    eventType,
                    eventKey,
                    queueName);
            if (publishToRetryQueue(message, workerName, eventType)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        long startedAt = System.nanoTime();
        try {
            WorkerHandleResult handleResult = handleByEventType(eventType, envelope.payload());
            if (handleResult.isSuccess()) {
                eventConsumeIdempotencyService.markSuccess(workerName, eventKey);
                recordMetrics(workerName, "success", startedAt);
                channel.basicAck(deliveryTag, false);
                return;
            }

            eventConsumeIdempotencyService.markSuccess(workerName, eventKey);
            String consumeResult = handleResult.isInvalid() ? "invalid" : "skipped";
            recordMetrics(workerName, consumeResult, startedAt);
            log.debug(
                    "[WORKER_CONSUMER] handled_without_side_effect worker={} eventType={} reason={}",
                    workerName,
                    eventType,
                    handleResult.getReason());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            eventConsumeIdempotencyService.releaseOnFailure(workerName, eventKey);
            recordMetrics(workerName, "failed", startedAt);
            log.warn(
                    "[WORKER_CONSUMER] consume_failed worker={} eventType={} eventKey={} queue={}",
                    workerName,
                    eventType,
                    eventKey,
                    queueName,
                    ex);
            if (publishToRetryQueue(message, workerName, eventType)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private boolean publishToRetryQueue(
            Message message, String workerName, WorkerEventType eventType) {
        try {
            rabbitMessageRepublisher.republishJsonBody(
                    message, rabbitProperties.getExchange(), rabbitProperties.getRetryRoutingKey());
            meterRegistry
                    .counter(
                            "worker_retry_total",
                            "worker",
                            workerName,
                            "eventType",
                            eventType.name(),
                            "result",
                            "published")
                    .increment();
            return true;
        } catch (Exception retryEx) {
            meterRegistry
                    .counter(
                            "worker_retry_total",
                            "worker",
                            workerName,
                            "eventType",
                            eventType.name(),
                            "result",
                            "publish_failed")
                    .increment();
            log.warn(
                    "[WORKER_CONSUMER] retry_publish_failed worker={} eventType={} retryRoutingKey={}",
                    workerName,
                    eventType,
                    rabbitProperties.getRetryRoutingKey(),
                    retryEx);
            return false;
        }
    }

    private WorkerHandleResult handleByEventType(WorkerEventType eventType, JsonNode payloadNode)
            throws Exception {
        if (eventType == WorkerEventType.AI_JOB_REQUESTED) {
            AiJobRequestedPayload payload = parseRequestedPayload(payloadNode);
            return aiJobRequestedWorker.handle(payload);
        }

        if (eventType == WorkerEventType.AI_JOB_COMPLETED
                || eventType == WorkerEventType.AI_JOB_FAILED) {
            AiJobResultPayload payload = parseResultPayload(payloadNode);
            return aiJobResultWorker.handle(payload);
        }

        return WorkerHandleResult.skipped("unsupported_event_type");
    }

    private AiJobRequestedPayload parseRequestedPayload(JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isNull()) {
            return null;
        }

        List<String> repoUrls = new ArrayList<>();
        JsonNode repoUrlsNode = firstNode(payloadNode, "repoUrls", "repo_urls");
        if (repoUrlsNode != null && repoUrlsNode.isArray()) {
            repoUrlsNode.forEach(node -> repoUrls.add(node.asText()));
        }

        Long versionNoLong = firstLong(payloadNode, "versionNo", "version_no");
        Integer versionNo = versionNoLong != null ? versionNoLong.intValue() : null;

        return new AiJobRequestedPayload(
                firstLong(payloadNode, "resumeId", "resume_id"),
                versionNo,
                firstLong(payloadNode, "userId", "user_id"),
                firstText(payloadNode, "positionName", "position_name"),
                repoUrls);
    }

    private AiJobResultPayload parseResultPayload(JsonNode payloadNode) throws Exception {
        if (payloadNode == null || payloadNode.isNull()) {
            return null;
        }

        Long userId = firstLong(payloadNode, "userId", "user_id");
        String payloadJson = objectMapper.writeValueAsString(payloadNode);
        String source = firstText(payloadNode, "source");
        Long resumeId = firstLong(payloadNode, "resumeId", "resume_id");
        Long versionNoLong = firstLong(payloadNode, "versionNo", "version_no");
        Integer versionNo = versionNoLong != null ? versionNoLong.intValue() : null;
        String status = firstText(payloadNode, "status");

        return new AiJobResultPayload(userId, payloadJson, source, resumeId, versionNo, status);
    }

    private void recordMetrics(String workerName, String result, long startedAtNanos) {
        meterRegistry
                .counter("worker_consume_total", "worker", workerName, "result", result)
                .increment();
        meterRegistry
                .timer("worker_handle_duration_seconds", "worker", workerName, "result", result)
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
    }

    private String resolveWorkerName(WorkerEventType eventType) {
        if (eventType == WorkerEventType.AI_JOB_REQUESTED) {
            return WORKER_AI_REQUESTED;
        }
        return WORKER_AI_RESULT;
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            String text = value.asText();
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private JsonNode firstNode(JsonNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private Long firstLong(JsonNode node, String... fields) {
        String text = firstText(node, fields);
        if (text == null) {
            return null;
        }

        try {
            return Long.parseLong(text);
        } catch (Exception ex) {
            return null;
        }
    }
}
