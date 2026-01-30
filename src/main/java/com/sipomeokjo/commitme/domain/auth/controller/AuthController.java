package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.LoginUrlResponse;
import com.sipomeokjo.commitme.domain.auth.service.AuthCommandService;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import com.sipomeokjo.commitme.security.CookieProperties;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthQueryService authQueryService;
    private final AuthCommandService authCommandService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @GetMapping("/github/loginUrl")
    public ResponseEntity<APIResponse<LoginUrlResponse>> getLoginUrl(HttpServletResponse response) {
        String state = authQueryService.generateState();

        ResponseCookie cookie =
                ResponseCookie.from("state", state)
                        .httpOnly(true)
                        .secure(cookieProperties.isSecure())
                        .sameSite("Lax")
                        .path("/auth/github")
                        .maxAge(Duration.ofMinutes(10))
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String loginUrl = authQueryService.getLoginUrl(state);
        LoginUrlResponse data = new LoginUrlResponse(loginUrl);
        return APIResponse.onSuccess(SuccessCode.LOGIN_URL_ISSUED, data);
    }

    @PostMapping("/token")
    public ResponseEntity<APIResponse<Void>> reissueToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        String accessToken = authCommandService.reissueAccessToken(refreshToken);

        ResponseCookie accessCookie =
                ResponseCookie.from("access_token", accessToken)
                        .httpOnly(true)
                        .secure(cookieProperties.isSecure())
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(jwtProperties.getAccessExpiration())
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return APIResponse.onSuccess(SuccessCode.ACCESS_TOKEN_REISSUED);
    }
}
