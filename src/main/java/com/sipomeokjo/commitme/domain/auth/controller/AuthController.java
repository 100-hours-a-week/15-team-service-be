package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.LoginUrlResponse;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import com.sipomeokjo.commitme.security.AuthLoginAuthenticationToken;
import com.sipomeokjo.commitme.security.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthQueryService authQueryService;
    private final CookieProperties cookieProperties;

    // 프로젝트에 있는 Provider + SuccessHandler를 태우기 위해 필요
    private final AuthenticationManager authenticationManager;
    private final AuthenticationSuccessHandler authLoginSuccessHandler;

    @GetMapping("/github/loginUrl")
    public ResponseEntity<APIResponse<LoginUrlResponse>> getLoginUrl(HttpServletResponse response) {
        String state = authQueryService.generateState();

        ResponseCookie cookie = ResponseCookie.from("state", state)
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

    /**
     * GitHub OAuth redirect URI
     * - state 쿠키 값과 query state를 비교
     * - code를 AuthLoginAuthenticationToken으로 넘기면 AuthLoginAuthenticationProvider가 처리
     * - 성공하면 AuthLoginSuccessHandler가 access_token/refresh_token 쿠키 세팅
     */
    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        String stateCookie = extractCookie(request, "state");
        if (stateCookie == null || !stateCookie.equals(state)) {
            response.setStatus(400);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Invalid state");
            return;
        }

        // ✅ 핵심: Provider가 supports 하는 Token 타입으로 authenticate 해야 함
        Authentication authRequest = AuthLoginAuthenticationToken.unauthenticated(code);
        Authentication authenticated = authenticationManager.authenticate(authRequest);

        authLoginSuccessHandler.onAuthenticationSuccess(request, response, authenticated);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
