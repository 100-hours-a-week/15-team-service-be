package com.sipomeokjo.commitme.domain.outbox.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OutboxEventTypes {
    public static final String AI_JOB_REQUESTED = "AIJobRequested";
    public static final String AI_JOB_COMPLETED = "AIJobCompleted";
    public static final String AI_JOB_FAILED = "AIJobFailed";
}
