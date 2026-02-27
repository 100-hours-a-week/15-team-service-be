package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.auth.dto.LoginUrlResponse;
import com.sipomeokjo.commitme.domain.auth.service.AuthCommandService;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthQueryService authQueryService;
    private final AuthCommandService authCommandService;
    private final AuthCookieWriter authCookieWriter;

    @GetMapping("/github/loginUrl")
    public ResponseEntity<APIResponse<LoginUrlResponse>> getLoginUrl(HttpServletResponse response) {
        String state = authQueryService.generateState();
        authCookieWriter.writeStateCookie(response, state);

        String loginUrl = authQueryService.getLoginUrl(state);
        LoginUrlResponse data = new LoginUrlResponse(loginUrl);
        return APIResponse.onSuccess(SuccessCode.LOGIN_URL_ISSUED, data);
    }

    @PostMapping("/token")
    public ResponseEntity<APIResponse<Void>> reissueToken(
            HttpServletRequest request, HttpServletResponse response) {
        List<String> refreshTokenCandidates = extractRefreshTokenCandidates(request);
        AuthTokenReissueResult tokenResult =
                authCommandService.reissueAccessToken(refreshTokenCandidates);
        authCookieWriter.writeAuthCookies(
                response, tokenResult.accessToken(), tokenResult.refreshToken());

        return APIResponse.onSuccess(SuccessCode.ACCESS_TOKEN_REISSUED);
    }

    private List<String> extractRefreshTokenCandidates(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return List.of();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }
}
