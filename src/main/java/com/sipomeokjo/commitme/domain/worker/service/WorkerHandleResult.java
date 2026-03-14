package com.sipomeokjo.commitme.domain.worker.service;

import lombok.Getter;

@Getter
public class WorkerHandleResult {
    private final boolean success;
    private final boolean invalid;
    private final String reason;

    private WorkerHandleResult(boolean success, boolean invalid, String reason) {
        this.success = success;
        this.invalid = invalid;
        this.reason = reason;
    }

    public static WorkerHandleResult success() {
        return new WorkerHandleResult(true, false, null);
    }

    public static WorkerHandleResult invalid(String reason) {
        return new WorkerHandleResult(false, true, reason);
    }

    public static WorkerHandleResult skipped(String reason) {
        return new WorkerHandleResult(false, false, reason);
    }
}
