package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.api.response.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.StringUtils;

@Slf4j
public class AuthLoginAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public AuthLoginAuthenticationFilter() {
        super(
                request -> {
                    String uri = request.getRequestURI();
                    if (!HttpMethod.GET.matches(request.getMethod())) {
                        return false;
                    }
                    return "/auth/github".equals(uri);
                });
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        log.info(
                "[Auth][Callback] start uri={} queryError={} hasCode={} hasState={}",
                request.getRequestURI(),
                request.getParameter("error"),
                StringUtils.hasText(request.getParameter("code")),
                StringUtils.hasText(request.getParameter("state")));

        String error = request.getParameter("error");
        if (StringUtils.hasText(error)) {
            if ("access_denied".equals(error)) {
                log.warn("[Auth][Callback] access_denied error={}", error);
                throw new AuthLoginAuthenticationException(ErrorCode.OAUTH_ACCESS_DENIED);
            }
            log.warn("[Auth][Callback] unexpected_error_param error={}", error);
            throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
        }

        String code = request.getParameter("code");
        String state = request.getParameter("state");
        List<String> stateCookies = extractStateCookies(request);
        log.info(
                "[Auth][Callback] state_cookie_candidates count={} stateQueryFingerprint={}",
                stateCookies.size(),
                fingerprint(state));

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            log.warn(
                    "[Auth][Callback] bad_request_missing_params hasCode={} hasState={}",
                    StringUtils.hasText(code),
                    StringUtils.hasText(state));
            throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
        }
        if (stateCookies.isEmpty() || stateCookies.stream().noneMatch(state::equals)) {
            log.warn(
                    "[Auth][Callback] bad_request_state_mismatch stateCookieCount={} stateQueryFingerprint={} stateCookieFingerprints={}",
                    stateCookies.size(),
                    fingerprint(state),
                    stateCookies.stream().map(this::fingerprint).toList());
            throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
        }

        AuthLoginAuthenticationToken authRequest =
                AuthLoginAuthenticationToken.unauthenticated(code);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private List<String> extractStateCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return List.of();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "state".equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String fingerprint(String value) {
        if (!StringUtils.hasText(value)) {
            return "empty";
        }
        int visible = Math.min(6, value.length());
        return "***" + value.substring(value.length() - visible);
    }
}
