package com.sipomeokjo.commitme.domain.userSetting.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsResponse;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsUpdateRequest;
import com.sipomeokjo.commitme.domain.userSetting.service.UserSettingCommandService;
import com.sipomeokjo.commitme.domain.userSetting.service.UserSettingQueryService;
import com.sipomeokjo.commitme.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingQueryService userSettingQueryService;
    private final UserSettingCommandService userSettingCommandService;

    @GetMapping
    public ResponseEntity<APIResponse<UserSettingsResponse>> getSettings(
            @CurrentUserId Long userId) {
        return APIResponse.onSuccess(SuccessCode.USER_SETTINGS_FETCHED,
				userSettingQueryService.getSettings(userId));
    }

    @PatchMapping
    public ResponseEntity<APIResponse<UserSettingsResponse>> updateSettings(
            @CurrentUserId Long userId,
            @RequestBody UserSettingsUpdateRequest request) {
        return APIResponse.onSuccess(SuccessCode.USER_SETTINGS_UPDATED,
				userSettingCommandService.updateSettings(userId, request));
    }
}
