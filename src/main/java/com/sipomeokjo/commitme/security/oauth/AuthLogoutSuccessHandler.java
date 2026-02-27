package com.sipomeokjo.commitme.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import com.sipomeokjo.commitme.domain.auth.service.AuthSessionIssueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final ObjectMapper objectMapper;
    private final AuthCookieWriter authCookieWriter;
    private final AuthSessionIssueService authSessionIssueService;

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        List<String> refreshTokens = extractCookieValues(request, REFRESH_TOKEN_COOKIE_NAME);
        for (String refreshToken : refreshTokens) {
            authSessionIssueService.revokeRefreshToken(refreshToken);
        }

        authCookieWriter.expireAuthCookies(response);
        authCookieWriter.expireCsrfCookie(response);

        APIResponse<Void> body = APIResponse.body(SuccessCode.LOGOUT_SUCCESS);
        response.setStatus(SuccessCode.LOGOUT_SUCCESS.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
        log.info("[Auth][Logout] 로그아웃 처리 완료");
    }

    private List<String> extractCookieValues(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return List.of();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }
}
