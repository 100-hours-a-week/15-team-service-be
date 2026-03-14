package com.sipomeokjo.commitme.domain.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.config.RabbitProperties;
import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventEnvelope;
import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEvent;
import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEventStatus;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublishProcessor {
    private static final List<OutboxEventStatus> READY_STATUSES =
            List.of(OutboxEventStatus.PENDING, OutboxEventStatus.RETRY);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties rabbitProperties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public List<Long> claimReadyEventIds(int batchSize) {
        Instant now = Instant.now();
        List<OutboxEvent> events =
                outboxEventRepository.findReadyEventsWithLock(READY_STATUSES, now, batchSize);
        for (OutboxEvent event : events) {
            event.markProcessing(now);
        }

        if (!events.isEmpty()) {
            meterRegistry.counter("outbox_publish_claim_total", "result", "success").increment();
        }

        return events.stream().map(OutboxEvent::getId).toList();
    }

    public void publishClaimedEvent(Long outboxEventId) {
        PublishSnapshot snapshot = loadPublishSnapshot(outboxEventId);
        if (snapshot == null) {
            meterRegistry
                    .counter("outbox_publish_attempt_total", "result", "not_found")
                    .increment();
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        String eventType = snapshot.eventType();
        try {
            OutboxEventEnvelope envelope =
                    objectMapper.readValue(snapshot.payload(), OutboxEventEnvelope.class);
            CorrelationData correlationData = new CorrelationData(envelope.eventId());
            rabbitTemplate.convertAndSend(
                    rabbitProperties.getExchange(),
                    rabbitProperties.getRoutingKey(),
                    snapshot.payload(),
                    message -> {
                        if (!envelope.eventId().isBlank()) {
                            message.getMessageProperties().setMessageId(envelope.eventId());
                        }
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    },
                    correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (confirm == null || !confirm.isAck()) {
                String reason = confirm == null ? "confirm is null" : confirm.getReason();
                throw new IllegalStateException("rabbit publish nacked: " + reason);
            }
            markPublished(outboxEventId);
            meterRegistry
                    .counter(
                            "outbox_publish_attempt_total",
                            "event_type",
                            eventType,
                            "result",
                            "success")
                    .increment();
        } catch (IllegalArgumentException ex) {
            markFailedImmediately(outboxEventId, ex.getMessage());
            meterRegistry
                    .counter(
                            "outbox_publish_attempt_total",
                            "event_type",
                            eventType,
                            "result",
                            "unsupported")
                    .increment();
            log.warn(
                    "[OUTBOX] publish_unsupported eventId={} eventType={}",
                    outboxEventId,
                    eventType,
                    ex);
        } catch (Exception ex) {
            OutboxEventStatus nextStatus = markRetryOrFailed(outboxEventId, ex.getMessage());
            String resultTag = nextStatus == OutboxEventStatus.FAILED ? "failed" : "retry";
            meterRegistry
                    .counter(
                            "outbox_publish_attempt_total",
                            "event_type",
                            eventType,
                            "result",
                            resultTag)
                    .increment();
            log.warn(
                    "[OUTBOX] publish_failed eventId={} eventType={} status={} attempt={}/{}",
                    outboxEventId,
                    eventType,
                    nextStatus,
                    snapshot.attemptCount(),
                    snapshot.maxAttempts(),
                    ex);
        } finally {
            sample.stop(
                    Timer.builder("outbox_publish_latency")
                            .tag("event_type", eventType)
                            .register(meterRegistry));
        }
    }

    protected PublishSnapshot loadPublishSnapshot(Long outboxEventId) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId).orElse(null);
        if (outboxEvent == null) {
            return null;
        }
        return new PublishSnapshot(
                outboxEvent.getId(),
                outboxEvent.getEventType(),
                outboxEvent.getPayload(),
                outboxEvent.getAttemptCount(),
                outboxEvent.getMaxAttempts());
    }

    protected void markPublished(Long outboxEventId) {
        transactionTemplate.executeWithoutResult(
                status ->
                        outboxEventRepository
                                .findById(outboxEventId)
                                .ifPresent(event -> event.markPublished(Instant.now())));
    }

    protected void markFailedImmediately(Long outboxEventId, String errorMessage) {
        transactionTemplate.executeWithoutResult(
                status ->
                        outboxEventRepository
                                .findById(outboxEventId)
                                .ifPresent(
                                        event ->
                                                event.markFailedImmediately(
                                                        errorMessage, Instant.now())));
    }

    protected OutboxEventStatus markRetryOrFailed(Long outboxEventId, String errorMessage) {
        return transactionTemplate.execute(
                status -> {
                    OutboxEvent outboxEvent =
                            outboxEventRepository.findById(outboxEventId).orElse(null);
                    if (outboxEvent == null) {
                        return OutboxEventStatus.FAILED;
                    }
                    Instant nextAttemptAt = Instant.now().plusSeconds(backoffSeconds(outboxEvent));
                    outboxEvent.markRetryOrFailed(errorMessage, nextAttemptAt);
                    return outboxEvent.getStatus();
                });
    }

    private long backoffSeconds(OutboxEvent outboxEvent) {
        int nextAttemptCount = outboxEvent.getAttemptCount() + 1;
        long backoff = (long) Math.pow(2, Math.max(0, nextAttemptCount - 1)) * 5L;
        return Math.min(backoff, Duration.ofMinutes(5).toSeconds());
    }

    protected record PublishSnapshot(
            Long outboxEventId,
            String eventType,
            String payload,
            int attemptCount,
            int maxAttempts) {}
}
