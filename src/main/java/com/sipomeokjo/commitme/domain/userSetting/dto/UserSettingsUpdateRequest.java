package com.sipomeokjo.commitme.domain.userSetting.dto;

public record UserSettingsUpdateRequest(
        Boolean notificationEnabled,
        Boolean interviewResumeDefaultsEnabled
) {
}
