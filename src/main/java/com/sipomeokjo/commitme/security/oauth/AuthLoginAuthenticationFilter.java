package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.api.response.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.StringUtils;

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
        String error = request.getParameter("error");
        if (StringUtils.hasText(error)) {
            if ("access_denied".equals(error)) {
                throw new AuthLoginAuthenticationException(ErrorCode.OAUTH_ACCESS_DENIED);
            }
            throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
        }

        String code = request.getParameter("code");
        String state = request.getParameter("state");
        List<String> stateCookies = extractStateCookies(request);

        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
        }
        if (stateCookies.isEmpty() || stateCookies.stream().noneMatch(state::equals)) {
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
}
