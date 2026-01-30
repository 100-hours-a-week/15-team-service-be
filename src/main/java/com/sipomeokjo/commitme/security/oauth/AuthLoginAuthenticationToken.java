package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class AuthLoginAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final Object credentials;

    private AuthLoginAuthenticationToken(Object principal, Object credentials) {
        super(Collections.emptyList());
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    private AuthLoginAuthenticationToken(Long userId, AuthLoginResult loginResult) {
        super(Collections.emptyList());
        this.principal = userId;
        this.credentials = null;
        setDetails(loginResult);
        setAuthenticated(true);
    }

    public static AuthLoginAuthenticationToken unauthenticated(String code) {
        return new AuthLoginAuthenticationToken(null, code);
    }

    public static AuthLoginAuthenticationToken authenticated(
            Long userId, AuthLoginResult loginResult) {
        return new AuthLoginAuthenticationToken(userId, loginResult);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
