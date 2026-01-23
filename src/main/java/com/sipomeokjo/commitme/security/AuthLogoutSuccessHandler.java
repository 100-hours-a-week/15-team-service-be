package com.sipomeokjo.commitme.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthLogoutSuccessHandler implements LogoutSuccessHandler {

	private final ObjectMapper objectMapper;
	private final CookieProperties cookieProperties;

	@Override
	public void onLogoutSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) throws IOException {
		ResponseCookie expireAccess = ResponseCookie.from("access_token", "")
				.httpOnly(true)
				.secure(cookieProperties.isSecure())
				.sameSite("Lax")
				.path("/")
				.maxAge(Duration.ZERO)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, expireAccess.toString());

		ResponseCookie expireRefresh = ResponseCookie.from("refresh_token", "")
				.httpOnly(true)
				.secure(cookieProperties.isSecure())
				.sameSite("Lax")
				.path("/auth/token")
				.maxAge(Duration.ZERO)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, expireRefresh.toString());

		APIResponse<Void> body = APIResponse.body(SuccessCode.LOGOUT_SUCCESS);
		response.setStatus(SuccessCode.LOGOUT_SUCCESS.getHttpStatus().value());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), body);
		log.info("[Auth][Logout] 로그아웃 처리 완료");
	}
}
