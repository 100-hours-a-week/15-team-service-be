package com.sipomeokjo.commitme.domain.outbox.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OutboxEventPublisherWorker {
    private static final int BATCH_SIZE = 50;

    private final OutboxEventPublishProcessor outboxEventPublishProcessor;
    private final MeterRegistry meterRegistry;
    private final long stuckProcessingTimeoutMs;

    public OutboxEventPublisherWorker(
            OutboxEventPublishProcessor outboxEventPublishProcessor,
            MeterRegistry meterRegistry,
            @Value("${app.outbox.stuck-processing-timeout-ms:300000}")
                    long stuckProcessingTimeoutMs) {
        this.outboxEventPublishProcessor = outboxEventPublishProcessor;
        this.meterRegistry = meterRegistry;
        this.stuckProcessingTimeoutMs = stuckProcessingTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    public void publish() {
        List<Long> eventIds = outboxEventPublishProcessor.claimReadyEventIds(BATCH_SIZE);
        if (eventIds.isEmpty()) {
            return;
        }

        meterRegistry.counter("outbox_publish_batch_total", "result", "claimed").increment();
        log.debug("[OUTBOX] publish_batch_started size={}", eventIds.size());

        for (Long eventId : eventIds) {
            outboxEventPublishProcessor.publishClaimedEvent(eventId);
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.stuck-recovery-delay-ms:60000}")
    public void recoverStuckProcessing() {
        outboxEventPublishProcessor.recoverStuckProcessingEvents(
                BATCH_SIZE, Duration.ofMillis(stuckProcessingTimeoutMs));
    }
}
