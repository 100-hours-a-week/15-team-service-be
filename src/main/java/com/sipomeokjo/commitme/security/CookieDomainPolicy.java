package com.sipomeokjo.commitme.security;

import com.sipomeokjo.commitme.config.CsrfProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieDomainPolicy {

    private final CookieProperties cookieProperties;
    private final CsrfProperties csrfProperties;

    public boolean isSecure() {
        return cookieProperties.isSecure();
    }

    public String authDomain() {
        return normalize(csrfProperties.cookieDomain());
    }

    public String csrfDomain() {
        return normalize(csrfProperties.cookieDomain());
    }

    private String normalize(String domain) {
        if (domain == null) {
            return null;
        }
        String trimmed = domain.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
