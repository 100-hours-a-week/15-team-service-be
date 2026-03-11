package com.sipomeokjo.commitme.domain.outbox.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 80)
    private String aggregateId;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public static OutboxEvent create(
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            int maxAttempts,
            Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.attemptCount = 0;
        event.maxAttempts = Math.max(maxAttempts, 1);
        event.nextAttemptAt = now;
        return event;
    }

    public void markProcessing(Instant now) {
        this.status = OutboxEventStatus.PROCESSING;
        this.lockedAt = now;
    }

    public void markPublished(Instant now) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = now;
        this.lockedAt = null;
        this.lastError = null;
    }

    public void markRetryOrFailed(String error, Instant nextAttemptAt) {
        this.attemptCount += 1;
        this.lockedAt = null;
        this.lastError = trimError(error);

        if (this.attemptCount >= this.maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
            this.nextAttemptAt = nextAttemptAt == null ? Instant.now() : nextAttemptAt;
            return;
        }

        this.status = OutboxEventStatus.RETRY;
        this.nextAttemptAt = nextAttemptAt == null ? Instant.now() : nextAttemptAt;
    }

    public void markFailedImmediately(String error, Instant now) {
        this.attemptCount = this.maxAttempts;
        this.status = OutboxEventStatus.FAILED;
        this.lockedAt = null;
        this.nextAttemptAt = now;
        this.lastError = trimError(error);
    }

    private String trimError(String error) {
        if (error == null || error.isBlank()) {
            return "unknown";
        }
        String message = error.trim();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
