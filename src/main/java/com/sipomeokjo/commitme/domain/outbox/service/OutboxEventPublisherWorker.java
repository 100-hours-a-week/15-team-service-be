package com.sipomeokjo.commitme.domain.outbox.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherWorker {
    private static final int BATCH_SIZE = 50;

    private final OutboxEventPublishProcessor outboxEventPublishProcessor;
    private final MeterRegistry meterRegistry;

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
}
