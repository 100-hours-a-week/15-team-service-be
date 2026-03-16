package com.sipomeokjo.commitme.domain.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventEnvelope;
import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEvent;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OutboxEventService {
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name:commitme}")
    private String producer;

    public void enqueue(
            String eventType, String aggregateType, String aggregateId, Object payload) {
        Instant now = Instant.now();
        try {
            OutboxEventEnvelope envelope =
                    new OutboxEventEnvelope(
                            UUID.randomUUID().toString(),
                            eventType,
                            producer,
                            eventType + ":" + aggregateType + ":" + aggregateId,
                            aggregateType,
                            aggregateId,
                            now,
                            objectMapper.valueToTree(payload));
            String payloadJson = objectMapper.writeValueAsString(envelope);
            OutboxEvent event =
                    OutboxEvent.create(
                            eventType,
                            aggregateType,
                            aggregateId,
                            payloadJson,
                            DEFAULT_MAX_ATTEMPTS,
                            now);
            outboxEventRepository.save(event);
            meterRegistry
                    .counter("outbox_enqueue_total", "event_type", eventType, "result", "success")
                    .increment();
        } catch (Exception ex) {
            meterRegistry
                    .counter("outbox_enqueue_total", "event_type", eventType, "result", "failed")
                    .increment();
            log.warn(
                    "[OUTBOX] enqueue_failed eventType={} aggregateType={} aggregateId={}",
                    eventType,
                    aggregateType,
                    aggregateId,
                    ex);
            throw new IllegalStateException("Failed to enqueue outbox event", ex);
        }
    }
}
