package com.sipomeokjo.commitme.domain.auth.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.auth.dto.LoginUrlResponse;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import com.sipomeokjo.commitme.security.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthQueryService authQueryService;
    private final CookieProperties cookieProperties;

    private final AuthenticationManager authenticationManager;
    private final AuthenticationSuccessHandler authLoginSuccessHandler;

    @GetMapping("/github/loginUrl")
    public ResponseEntity<APIResponse<LoginUrlResponse>> getLoginUrl(HttpServletResponse response) {
        String state = authQueryService.generateState();

        ResponseCookie cookie = ResponseCookie.from("state", state)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path("/auth/github") // ✅ 콜백(/auth/github/callback)에도 전송됨
                .maxAge(Duration.ofMinutes(10))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String loginUrl = authQueryService.getLoginUrl(state);
        return APIResponse.onSuccess(SuccessCode.LOGIN_URL_ISSUED, new LoginUrlResponse(loginUrl));
    }

    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        String stateCookie = extractCookie(request, "state");
        if (stateCookie == null || !stateCookie.equals(state)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Invalid state");
            return;
        }

        // provider가 code로 token exchange + user upsert까지 처리한다고 가정
        Authentication authRequest = new UsernamePasswordAuthenticationToken("GITHUB", code);
        Authentication authenticated = authenticationManager.authenticate(authRequest);

        // ✅ 여기서 access_token / refresh_token 쿠키 세팅됨
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
