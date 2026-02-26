package com.sipomeokjo.commitme.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

public class LoggingCsrfTokenRepository implements CsrfTokenRepository {

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
        return delegate.loadToken(request);
    }
}
