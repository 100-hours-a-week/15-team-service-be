package com.sipomeokjo.commitme.domain.worker.service;

import com.sipomeokjo.commitme.domain.worker.entity.EventConsumeLog;
import com.sipomeokjo.commitme.domain.worker.entity.EventConsumeStatus;
import com.sipomeokjo.commitme.domain.worker.repository.EventConsumeLogRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumeIdempotencyService {
    private final EventConsumeLogRepository eventConsumeLogRepository;

    @Value("${app.worker.consume-log-lease-ms:30000}")
    private long leaseMillis;

    @Transactional
    public EventConsumeStartResult tryStart(String consumer, String eventId, String queueName) {
        Instant now = Instant.now();
        Instant leaseExpiresAt = now.plusMillis(Math.max(leaseMillis, 1000L));
        int inserted =
                eventConsumeLogRepository.insertProcessingIgnore(
                        consumer, eventId, queueName, now, leaseExpiresAt);
        if (inserted > 0) {
            return EventConsumeStartResult.STARTED;
        }

        EventConsumeLog existing =
                eventConsumeLogRepository.findByConsumerAndEventId(consumer, eventId).orElse(null);
        if (existing == null) {
            int retriedInsert =
                    eventConsumeLogRepository.insertProcessingIgnore(
                            consumer, eventId, queueName, now, leaseExpiresAt);
            return retriedInsert > 0
                    ? EventConsumeStartResult.STARTED
                    : EventConsumeStartResult.IN_PROGRESS;
        }

        if (existing.getStatus() == EventConsumeStatus.SUCCESS) {
            log.debug(
                    "[WORKER_CONSUME] duplicate_event_succeeded consumer={} eventId={} queue={}",
                    consumer,
                    eventId,
                    queueName);
            return EventConsumeStartResult.ALREADY_SUCCEEDED;
        }

        if (!existing.isLeaseExpired(now)) {
            log.debug(
                    "[WORKER_CONSUME] duplicate_event_in_progress consumer={} eventId={} queue={} leaseExpiresAt={}",
                    consumer,
                    eventId,
                    queueName,
                    existing.getLeaseExpiresAt());
            return EventConsumeStartResult.IN_PROGRESS;
        }

        int reclaimed =
                eventConsumeLogRepository.reclaimExpiredProcessing(
                        consumer, eventId, queueName, now, now, leaseExpiresAt);
        if (reclaimed > 0) {
            log.warn(
                    "[WORKER_CONSUME] reclaimed_stale_processing consumer={} eventId={} queue={} previousLeaseExpiresAt={}",
                    consumer,
                    eventId,
                    queueName,
                    existing.getLeaseExpiresAt());
            return EventConsumeStartResult.STARTED;
        }

        EventConsumeLog refreshed =
                eventConsumeLogRepository.findByConsumerAndEventId(consumer, eventId).orElse(null);
        if (refreshed != null && refreshed.getStatus() == EventConsumeStatus.SUCCESS) {
            return EventConsumeStartResult.ALREADY_SUCCEEDED;
        }
        return EventConsumeStartResult.IN_PROGRESS;
    }

    @Transactional
    public void markSuccess(String consumer, String eventId) {
        int updated = eventConsumeLogRepository.markSuccess(consumer, eventId, Instant.now());
        if (updated == 0) {
            log.warn(
                    "[WORKER_CONSUME] mark_success_skipped consumer={} eventId={}",
                    consumer,
                    eventId);
        }
    }

    @Transactional
    public void releaseOnFailure(String consumer, String eventId) {
        eventConsumeLogRepository.deleteByConsumerAndEventId(consumer, eventId);
    }
}
