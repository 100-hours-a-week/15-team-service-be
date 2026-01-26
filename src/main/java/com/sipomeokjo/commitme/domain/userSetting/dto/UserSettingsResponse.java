package com.sipomeokjo.commitme.domain.userSetting.dto;

public record UserSettingsResponse(
        Long userId,
        boolean notificationEnabled,
        boolean interviewResumeDefaultsEnabled
) {
}
