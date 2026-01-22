package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.LoginUrlResponse;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
	
	private final AuthQueryService authQueryService;

	@GetMapping("/github/loginUrl")
	public ResponseEntity<ApiResponse<LoginUrlResponse>> getLoginUrl(HttpServletResponse response) {
		String state = authQueryService.generateState();
		
		ResponseCookie cookie = ResponseCookie.from("state", state)
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.path("/auth/github")
				.maxAge(Duration.ofMinutes(10))
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		
		String loginUrl = authQueryService.getLoginUrl(state);
		LoginUrlResponse data = new LoginUrlResponse(loginUrl);
		return ApiResponse.onSuccess(SuccessCode.LOGIN_URL_ISSUED, data);
	}
	
}
