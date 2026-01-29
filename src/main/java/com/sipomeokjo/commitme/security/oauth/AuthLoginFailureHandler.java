package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.config.AuthRedirectProperties;
import com.sipomeokjo.commitme.security.CookieProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class AuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final AuthRedirectProperties authRedirectProperties;
    private final CookieProperties cookieProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        ResponseCookie expireState =
                ResponseCookie.from("state", "")
                        .httpOnly(true)
                        .secure(cookieProperties.isSecure())
                        .sameSite("Lax")
                        .path("/auth/github")
                        .maxAge(Duration.ZERO)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expireState.toString());
        response.sendRedirect(buildFailRedirectUrl());
    }

    private String buildFailRedirectUrl() {
        return UriComponentsBuilder.fromUriString(authRedirectProperties.redirectUri())
                .queryParam("status", "fail")
                .toUriString();
    }
}
