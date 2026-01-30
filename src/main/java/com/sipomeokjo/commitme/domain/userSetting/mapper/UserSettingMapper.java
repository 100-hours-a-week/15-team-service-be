package com.sipomeokjo.commitme.domain.userSetting.mapper;

import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsResponse;
import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import org.springframework.stereotype.Component;

@Component
public class UserSettingMapper {

    public UserSettingsResponse toResponse(UserSetting setting) {
        if (setting == null) {
            return null;
        }
        return new UserSettingsResponse(
                setting.getId(),
                setting.isNotificationEnabled(),
                setting.isInterviewResumeDefaultsEnabled());
    }
}
