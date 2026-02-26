package com.sipomeokjo.commitme.domain.user.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserProfileResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.service.UserCommandService;
import com.sipomeokjo.commitme.domain.user.service.UserQueryService;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final AuthCookieWriter authCookieWriter;
    private final AccessTokenProvider accessTokenProvider;

    @GetMapping
    public ResponseEntity<APIResponse<UserProfileResponse>> getProfile(@CurrentUserId Long userId) {
        UserProfileResponse response = userQueryService.getUserProfile(userId);
        return APIResponse.onSuccess(SuccessCode.USER_PROFILE_FETCHED, response);
    }

    @PatchMapping
    public ResponseEntity<APIResponse<UserUpdateResponse>> updateProfile(
            @CurrentUserId Long userId, @RequestBody UserUpdateRequest request) {
        UserUpdateResponse response = userCommandService.updateProfile(userId, request);
        return APIResponse.onSuccess(SuccessCode.USER_PROFILE_UPDATED, response);
    }

    @PostMapping("/onboarding")
    public ResponseEntity<APIResponse<OnboardingResponse>> onboard(
            @CurrentUserId Long userId,
            @RequestBody OnboardingRequest request,
            HttpServletResponse httpResponse) {
        OnboardingResponse onboardingResponse = userCommandService.onboard(userId, request);
        String accessToken =
                accessTokenProvider.createAccessToken(userId, onboardingResponse.status());
        authCookieWriter.writeAccessTokenCookie(httpResponse, accessToken);
        return APIResponse.onSuccess(SuccessCode.ONBOARDING_COMPLETED, onboardingResponse);
    }

    @DeleteMapping
    public ResponseEntity<APIResponse<Void>> deactivate(
            @CurrentUserId Long userId, HttpServletResponse httpResponse) {
        userCommandService.deactivate(userId);
        authCookieWriter.expireAuthCookies(httpResponse);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
