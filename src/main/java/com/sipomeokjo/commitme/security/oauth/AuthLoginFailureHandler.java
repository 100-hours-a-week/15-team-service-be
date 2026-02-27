package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.config.AuthRedirectProperties;
import com.sipomeokjo.commitme.domain.auth.service.AuthCookieWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final AuthRedirectProperties authRedirectProperties;
    private final AuthCookieWriter authCookieWriter;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        log.warn(
                "[Auth][LoginFailure] uri={} reason={} message={}",
                request.getRequestURI(),
                resolveReason(exception),
                exception.getMessage());
        authCookieWriter.expireStateCookie(response);
        response.sendRedirect(buildFailRedirectUrl(exception));
    }

    private String buildFailRedirectUrl(AuthenticationException exception) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(authRedirectProperties.redirectUri())
                        .queryParam("status", "fail");
        if (exception instanceof AuthLoginAuthenticationException authException) {
            builder.queryParam("reason", authException.getErrorCode().name());
        }
        return builder.toUriString();
    }

    private String resolveReason(AuthenticationException exception) {
        if (exception instanceof AuthLoginAuthenticationException authException) {
            return authException.getErrorCode().name();
        }
        return "AUTHENTICATION_EXCEPTION";
    }
}
