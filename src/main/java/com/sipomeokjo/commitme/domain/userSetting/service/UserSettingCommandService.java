package com.sipomeokjo.commitme.domain.userSetting.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsResponse;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsUpdateRequest;
import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import com.sipomeokjo.commitme.domain.userSetting.mapper.UserSettingMapper;
import com.sipomeokjo.commitme.domain.userSetting.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingCommandService {

    private final UserSettingRepository userSettingRepository;
    private final UserSettingMapper userSettingMapper;

    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_SETTINGS_NOT_FOUND));

        setting.update(request.notificationEnabled(), request.interviewResumeDefaultsEnabled());
        return userSettingMapper.toResponse(setting);
    }
}
