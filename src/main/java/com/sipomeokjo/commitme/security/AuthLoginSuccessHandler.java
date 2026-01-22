package com.sipomeokjo.commitme.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.dto.LoginResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final JwtProperties jwtProperties;
	private final ObjectMapper objectMapper;

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException {
		Object details = authentication.getDetails();
		if (!(details instanceof AuthLoginResult(String accessToken, String refreshToken, boolean onboardingCompleted))) {
			response.setStatus(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value());
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			ApiResponse<Void> body = new ApiResponse<>(
					ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
					ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
					null
			);
			objectMapper.writeValue(response.getOutputStream(), body);
			return;
		}

		ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.path("/")
				.maxAge(jwtProperties.getAccessExpiration())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.path("/auth/token")
				.maxAge(jwtProperties.getRefreshExpiration())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

		ResponseCookie expireState = ResponseCookie.from("state", "")
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.path("/auth/github")
				.maxAge(Duration.ZERO)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, expireState.toString());

		LoginResultResponse data = new LoginResultResponse(onboardingCompleted);
		ApiResponse<LoginResultResponse> body = new ApiResponse<>(
				SuccessCode.LOGIN_SUCCESS.getCode(),
				SuccessCode.LOGIN_SUCCESS.getMessage(),
				data
		);
		response.setStatus(SuccessCode.LOGIN_SUCCESS.getHttpStatus().value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
