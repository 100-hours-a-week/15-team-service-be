package com.sipomeokjo.commitme.domain.user.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.service.UserCommandService;
import com.sipomeokjo.commitme.security.AccessTokenProvider;
import com.sipomeokjo.commitme.security.CookieProperties;
import com.sipomeokjo.commitme.security.CustomUserDetails;
import com.sipomeokjo.commitme.security.JwtProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

	private final UserCommandService userCommandService;
	private final AccessTokenProvider accessTokenProvider;
	private final JwtProperties jwtProperties;
	private final CookieProperties cookieProperties;

	@PostMapping("/onboarding")
	public ResponseEntity<APIResponse<OnboardingResponse>> onboard(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestBody OnboardingRequest request,
			HttpServletResponse httpResponse) {
		Long userId = userDetails instanceof CustomUserDetails details ? details.userId() : null;
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		
		OnboardingResponse onboardingResponse = userCommandService.onboard(userId, request);
		String accessToken = accessTokenProvider.createAccessToken(userId, onboardingResponse.status());
		ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
				.httpOnly(true)
				.secure(cookieProperties.isSecure())
				.sameSite("Lax")
				.path("/")
				.maxAge(jwtProperties.getAccessExpiration())
				.build();
		httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
		return APIResponse.onSuccess(SuccessCode.ONBOARDING_COMPLETED, onboardingResponse);
	}
}
