package com.sipomeokjo.commitme.domain.loadtest.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCacheEvictRequest;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCacheEvictResponse;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCleanupRequest;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCleanupResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeBulkSeedRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeBulkSeedResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeCallbackReplayRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeCallbackReplayResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeResetRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeResetResponse;
import com.sipomeokjo.commitme.domain.loadtest.service.LoadtestService;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/loadtest")
@RequiredArgsConstructor
public class LoadtestController {

    private final LoadtestService loadtestService;
    private final AuthCookieWriter authCookieWriter;

    @PostMapping("/auth/users/signup")
    public ResponseEntity<APIResponse<LoadtestAuthSignupResponse>> signup(
            @RequestBody LoadtestAuthSignupRequest request) {

        return APIResponse.onSuccess(SuccessCode.CREATED, loadtestService.signupPending(request));
    }

    @PostMapping("/auth/users/bulk-create")
    public ResponseEntity<APIResponse<LoadtestAuthBulkCreateResponse>> bulkCreate(
            @RequestBody LoadtestAuthBulkCreateRequest request) {

        return APIResponse.onSuccess(SuccessCode.CREATED, loadtestService.bulkCreate(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<APIResponse<LoadtestAuthLoginResponse>> login(
            @RequestBody LoadtestAuthLoginRequest request, HttpServletResponse response) {
        LoadtestAuthLoginResponse data = loadtestService.login(request);
        authCookieWriter.writeAuthCookies(
                response, data.cookieAccessToken(), data.cookieRefreshToken());
        return APIResponse.onSuccess(SuccessCode.LOGIN_SUCCESS, data);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<APIResponse<LoadtestAuthLogoutResponse>> logout(
            @RequestBody LoadtestAuthLogoutRequest request, HttpServletResponse response) {
        LoadtestAuthLogoutResponse data = loadtestService.logout(request);
        authCookieWriter.expireAuthCookies(response);
        return APIResponse.onSuccess(SuccessCode.LOGOUT_SUCCESS, data);
    }

    @PostMapping("/auth/reset")
    public ResponseEntity<APIResponse<LoadtestAuthResetResponse>> reset(
            @RequestBody LoadtestAuthResetRequest request) {

        return APIResponse.onSuccess(SuccessCode.OK, loadtestService.reset(request));
    }

    @PostMapping("/cleanup")
    public ResponseEntity<APIResponse<LoadtestCleanupResponse>> cleanup(
            @RequestBody LoadtestCleanupRequest request) {

        return APIResponse.onSuccess(SuccessCode.OK, loadtestService.cleanup(request));
    }

    @PostMapping("/cache/evict")
    public ResponseEntity<APIResponse<LoadtestCacheEvictResponse>> evictCache(
            @RequestBody LoadtestCacheEvictRequest request) {

        return APIResponse.onSuccess(SuccessCode.OK, loadtestService.evictCache(request));
    }

    @PostMapping("/resumes")
    public ResponseEntity<APIResponse<JsonNode>> create(@RequestBody ResumeCreateRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.OK, loadtestService.requestResumeGenerate(request));
    }

    @PostMapping("/resumes/bulk-seed")
    public ResponseEntity<APIResponse<LoadtestResumeBulkSeedResponse>> bulkSeedResumes(
            @RequestBody LoadtestResumeBulkSeedRequest request) {

        return APIResponse.onSuccess(SuccessCode.CREATED, loadtestService.bulkSeedResumes(request));
    }

    @PostMapping("/resumes/callback-replay")
    public ResponseEntity<APIResponse<LoadtestResumeCallbackReplayResponse>> replayResumeCallbacks(
            @RequestBody LoadtestResumeCallbackReplayRequest request) {

        return APIResponse.onSuccess(
                SuccessCode.OK, loadtestService.replayResumeCallbacks(request));
    }

    @PostMapping("/resumes/reset")
    public ResponseEntity<APIResponse<LoadtestResumeResetResponse>> resetResumes(
            @RequestBody LoadtestResumeResetRequest request) {

        return APIResponse.onSuccess(SuccessCode.OK, loadtestService.resetResumes(request));
    }
}
