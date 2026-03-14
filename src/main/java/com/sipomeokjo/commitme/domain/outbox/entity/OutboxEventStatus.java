package com.sipomeokjo.commitme.domain.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    RETRY,
    PUBLISHED,
    FAILED
}
