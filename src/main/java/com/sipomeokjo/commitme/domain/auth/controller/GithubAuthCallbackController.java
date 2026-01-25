/*
package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.service.AuthCommandService;
import com.sipomeokjo.commitme.security.CookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/github")
public class GithubAuthCallbackController {

    private final AuthCommandService authCommandService;
    private final CookieProperties cookieProperties;

    @GetMapping("/callback")
    public void callback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        AuthLoginResult result = authCommandService.loginWithGithub(code);

        // JwtFilter가 읽는 쿠키 이름: access_token
        ResponseCookie accessCookie = ResponseCookie.from("access_token", result.accessToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path("/")
                // accessExpiration은 JwtProperties에서 관리하지만 여기선 대략값으로 OK
                .maxAge(Duration.ofMinutes(30))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", result.refreshToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 프론트로 돌려보내기 (원하는 주소로 수정)
        response.setStatus(302);
        response.setHeader("Location", "http://localhost:5173/");
    }
}
*/