package com.sipomeokjo.commitme.domain.worker.dto;

import java.util.Arrays;

public enum WorkerEventType {
    AI_JOB_REQUESTED("AIJobRequested"),
    AI_JOB_COMPLETED("AIJobCompleted"),
    AI_JOB_FAILED("AIJobFailed");

    private final String value;

    WorkerEventType(String value) {
        this.value = value;
    }

    public static WorkerEventType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
