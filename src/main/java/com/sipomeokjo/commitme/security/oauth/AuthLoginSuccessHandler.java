package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.config.AuthRedirectProperties;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthCookieWriter authCookieWriter;
    private final AuthRedirectProperties authRedirectProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        Object details = authentication.getDetails();
        if (!(details
                instanceof
                AuthLoginResult(
                        String accessToken,
                        String refreshToken,
                        boolean onboardingCompleted))) {
            log.warn(
                    "[Auth][LoginSuccess] 인증 상세값 타입 불일치: 사유=AuthLoginResult 아님, detailsType={}",
                    details == null ? "null" : details.getClass().getName());
            response.sendRedirect(buildRedirectUrl("fail", null));
            return;
        }

        authCookieWriter.writeAuthCookies(response, accessToken, refreshToken);
        authCookieWriter.expireStateCookie(response);

        response.sendRedirect(buildRedirectUrl("success", onboardingCompleted));
    }

    private String buildRedirectUrl(String status, Boolean onboardingCompleted) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(authRedirectProperties.redirectUri())
                        .queryParam("status", status);
        if (onboardingCompleted != null) {
            builder.queryParam("onboardingCompleted", onboardingCompleted);
        }
        return builder.toUriString();
    }
}
