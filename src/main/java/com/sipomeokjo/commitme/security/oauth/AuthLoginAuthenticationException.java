package com.sipomeokjo.commitme.security.oauth;

import com.sipomeokjo.commitme.api.response.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class AuthLoginAuthenticationException extends AuthenticationException {

    private final ErrorCode errorCode;

    public AuthLoginAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
