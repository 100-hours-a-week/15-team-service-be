package com.sipomeokjo.commitme.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLoginFailureHandler implements AuthenticationFailureHandler {

	public static final String ERROR_CODE_ATTRIBUTE = "authErrorCode";

	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception
	) throws IOException {
		ErrorCode errorCode = null;
		Object value = request.getAttribute(ERROR_CODE_ATTRIBUTE);
		if (value instanceof ErrorCode) {
			errorCode = (ErrorCode) value;
		} else if (exception instanceof AuthLoginAuthenticationException authException) {
			errorCode = authException.getErrorCode();
		}
		if (errorCode == null) {
			errorCode = ErrorCode.UNAUTHORIZED;
		}

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		APIResponse<Void> body = APIResponse.body(errorCode);
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
