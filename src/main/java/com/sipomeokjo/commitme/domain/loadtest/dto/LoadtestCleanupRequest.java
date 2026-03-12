package com.sipomeokjo.commitme.domain.loadtest.dto;

public record LoadtestCleanupRequest(
        String runId, Boolean deleteResumes, Boolean deleteNotifications, Boolean deleteUsers) {}
