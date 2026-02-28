package com.sipomeokjo.commitme.api.sse.dto;

public enum UserSseEventName {
    CONNECTED("connected"),
    NOTIFICATION("notification"),
    RESUME_REFRESH_REQUIRED("resume-refresh-required");

    private final String value;

    UserSseEventName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
