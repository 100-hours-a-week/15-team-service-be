package com.sipomeokjo.commitme.security;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.service.AuthCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLoginAuthenticationProvider implements AuthenticationProvider {

	private final AuthCommandService authCommandService;
	private final AccessTokenProvider accessTokenProvider;

	@Override
	public Authentication authenticate(Authentication authentication) {
		Object credentials = authentication.getCredentials();
		if (!(credentials instanceof String code) || code.isBlank()) {
			throw new AuthLoginAuthenticationException(ErrorCode.BAD_REQUEST);
		}

		try {
			AuthLoginResult result = authCommandService.loginWithGithub(code);
			Long userId = accessTokenProvider.getUserId(result.accessToken());
			return AuthLoginAuthenticationToken.authenticated(userId, result);
		} catch (BusinessException e) {
			throw new AuthLoginAuthenticationException(e.getErrorCode());
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return AuthLoginAuthenticationToken.class.isAssignableFrom(authentication);
	}
}
