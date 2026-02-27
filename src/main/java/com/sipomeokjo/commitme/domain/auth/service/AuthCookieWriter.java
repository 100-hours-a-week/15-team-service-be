package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.security.CookieDomainPolicy;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCookieWriter {

    private static final String SAME_SITE = "Lax";
    private static final String STATE_COOKIE_NAME = "state";
    private static final String STATE_COOKIE_PATH = "/auth/github";
    private static final Duration STATE_COOKIE_MAX_AGE = Duration.ofMinutes(10);

    private final CookieDomainPolicy cookieDomainPolicy;
    private final JwtProperties jwtProperties;

    public void writeAuthCookies(
            HttpServletResponse response, String accessToken, String refreshToken) {
        writeAccessTokenCookie(response, accessToken);
        writeRefreshTokenCookie(response, refreshToken);
    }

    public void writeAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, "access_token", accessToken, "/", jwtProperties.getAccessExpiration());
    }

    public void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookie(
                response,
                "refresh_token",
                refreshToken,
                "/auth/token",
                jwtProperties.getRefreshExpiration());
    }

    public void expireAuthCookies(HttpServletResponse response) {
        expireAccessTokenCookie(response);
        expireRefreshTokenCookie(response);
    }

    public void expireAccessTokenCookie(HttpServletResponse response) {
        addCookie(response, "access_token", "", "/", Duration.ZERO);
    }

    public void expireRefreshTokenCookie(HttpServletResponse response) {
        addCookie(response, "refresh_token", "", "/auth/token", Duration.ZERO);
    }

    public void writeStateCookie(HttpServletResponse response, String state) {
        addCookie(response, STATE_COOKIE_NAME, state, STATE_COOKIE_PATH, STATE_COOKIE_MAX_AGE);
    }

    public void expireStateCookie(HttpServletResponse response) {
        addCookie(response, STATE_COOKIE_NAME, "", STATE_COOKIE_PATH, Duration.ZERO);
    }

    private void addCookie(
            HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .secure(cookieDomainPolicy.isSecure())
                        .sameSite(SAME_SITE)
                        .path(path)
                        .maxAge(maxAge);
        String domain = cookieDomainPolicy.authDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        ResponseCookie cookie = builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
