package com.sipomeokjo.commitme.security.csrf;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

@Slf4j
public class LoggingCsrfTokenRepository implements CsrfTokenRepository {

    private static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";

    private final CsrfTokenRepository delegate;

    public LoggingCsrfTokenRepository(CsrfTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(
            CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            return;
        }
        delegate.saveToken(token, request, response);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        CsrfToken loaded = delegate.loadToken(request);
        String headerName = loaded != null ? loaded.getHeaderName() : DEFAULT_CSRF_HEADER_NAME;
        String parameterName =
                loaded != null ? loaded.getParameterName() : DEFAULT_CSRF_PARAMETER_NAME;
        String requestToken = request.getHeader(headerName);

        if (requestToken == null || requestToken.isBlank()) {
            return loaded;
        }

        if (loaded != null && requestToken.equals(loaded.getToken())) {
            return loaded;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return loaded;
        }

        List<String> candidates =
                Arrays.stream(cookies)
                        .filter(cookie -> DEFAULT_CSRF_COOKIE_NAME.equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .filter(value -> !value.isBlank())
                        .toList();

        if (candidates.size() <= 1) {
            return loaded;
        }

        if (candidates.stream().anyMatch(requestToken::equals)) {
            log.debug(
                    "[CSRF] duplicated_cookie_resolved method={} uri={} count={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    candidates.size());
            return new DefaultCsrfToken(headerName, parameterName, requestToken);
        }
        return loaded;
    }
}
