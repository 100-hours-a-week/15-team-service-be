package com.sipomeokjo.commitme.domain.worker.entity;

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
@Table(name = "event_consume_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventConsumeLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "queue_name", nullable = false, length = 150)
    private String queueName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventConsumeStatus status;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public static EventConsumeLog processing(
            String consumer, String eventId, String queueName, Instant leaseExpiresAt) {
        EventConsumeLog log = new EventConsumeLog();
        log.consumer = consumer;
        log.eventId = eventId;
        log.queueName = queueName;
        log.status = EventConsumeStatus.PROCESSING;
        log.leaseExpiresAt = leaseExpiresAt;
        return log;
    }

    public void markSuccess() {
        this.status = EventConsumeStatus.SUCCESS;
        this.leaseExpiresAt = null;
        this.processedAt = Instant.now();
    }

    public boolean isLeaseExpired(Instant now) {
        return leaseExpiresAt != null && !leaseExpiresAt.isAfter(now);
    }
}
