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
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
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
        expireCookieWithDomainVariants(response, "refresh_token", "/auth/token", true);
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
        expireCookieWithDomainVariants(response, "access_token", "/", true);
    }

    public void expireRefreshTokenCookie(HttpServletResponse response) {
        expireCookieWithDomainVariants(response, "refresh_token", "/auth/token", true);
    }

    public void expireCsrfCookie(HttpServletResponse response) {
        expireCookieWithDomainVariants(response, CSRF_COOKIE_NAME, "/", false);
    }

    public void writeStateCookie(HttpServletResponse response, String state) {
        expireCookieWithDomainVariants(response, STATE_COOKIE_NAME, STATE_COOKIE_PATH, true);
        addCookie(response, STATE_COOKIE_NAME, state, STATE_COOKIE_PATH, STATE_COOKIE_MAX_AGE);
    }

    public void expireStateCookie(HttpServletResponse response) {
        expireCookieWithDomainVariants(response, STATE_COOKIE_NAME, STATE_COOKIE_PATH, true);
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

    private void expireCookieWithDomainVariants(
            HttpServletResponse response, String name, String path, boolean httpOnly) {
        addCookie(response, name, "", path, Duration.ZERO, null, httpOnly);

        String domain = cookieDomainPolicy.authDomain();
        if (domain != null && !domain.isBlank()) {
            addCookie(response, name, "", path, Duration.ZERO, domain, httpOnly);
        }
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            String path,
            Duration maxAge,
            String domain,
            boolean httpOnly) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(name, value)
                        .httpOnly(httpOnly)
                        .secure(cookieDomainPolicy.isSecure())
                        .sameSite(SAME_SITE)
                        .path(path)
                        .maxAge(maxAge);
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
