package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.security.CookieProperties;
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

    private final CookieProperties cookieProperties;
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

    private void addCookie(
            HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie cookie =
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .secure(cookieProperties.isSecure())
                        .sameSite(SAME_SITE)
                        .path(path)
                        .maxAge(maxAge)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
