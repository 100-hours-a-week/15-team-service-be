package com.sipomeokjo.commitme.domain.loadtest.auth.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthBulkCreateRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthBulkCreateResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLoginRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLoginResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLogoutRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLogoutResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthResetRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthResetResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthSignupRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthSignupResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.service.LoadtestAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/loadtest/auth")
@RequiredArgsConstructor
public class LoadtestAuthController {

    private final LoadtestAuthService loadtestAuthService;
    private final AuthCookieWriter authCookieWriter;

    @PostMapping("/users/signup")
    public ResponseEntity<APIResponse<LoadtestAuthSignupResponse>> signup(
            @RequestBody LoadtestAuthSignupRequest request) {
        LoadtestAuthSignupResponse data = loadtestAuthService.signupPending(request);
        return APIResponse.onSuccess(SuccessCode.CREATED, data);
    }

    @PostMapping("/users/bulk-create")
    public ResponseEntity<APIResponse<LoadtestAuthBulkCreateResponse>> bulkCreate(
            @RequestBody LoadtestAuthBulkCreateRequest request) {
        LoadtestAuthBulkCreateResponse data = loadtestAuthService.bulkCreate(request);
        return APIResponse.onSuccess(SuccessCode.CREATED, data);
    }

    @PostMapping("/login")
    public ResponseEntity<APIResponse<LoadtestAuthLoginResponse>> login(
            @RequestBody LoadtestAuthLoginRequest request, HttpServletResponse response) {
        LoadtestAuthLoginResponse data = loadtestAuthService.login(request);
        authCookieWriter.writeAuthCookies(
                response, data.cookieAccessToken(), data.cookieRefreshToken());
        return APIResponse.onSuccess(SuccessCode.LOGIN_SUCCESS, data);
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<LoadtestAuthLogoutResponse>> logout(
            @RequestBody LoadtestAuthLogoutRequest request, HttpServletResponse response) {
        LoadtestAuthLogoutResponse data = loadtestAuthService.logout(request);
        authCookieWriter.expireAuthCookies(response);
        return APIResponse.onSuccess(SuccessCode.LOGOUT_SUCCESS, data);
    }

    @PostMapping("/reset")
    public ResponseEntity<APIResponse<LoadtestAuthResetResponse>> reset(
            @RequestBody LoadtestAuthResetRequest request) {
        LoadtestAuthResetResponse data = loadtestAuthService.reset(request);
        return APIResponse.onSuccess(SuccessCode.OK, data);
    }
}
