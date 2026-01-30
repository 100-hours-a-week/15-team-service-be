package com.sipomeokjo.commitme.security.jwt;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final AccessTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && tokenProvider.validateToken(token)) {
            String statusValue = tokenProvider.getStatus(token);
            UserStatus status = parseStatus(statusValue);
            if (status != null) {
                List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + status.name()));

                CustomUserDetails customUserDetails =
                        new CustomUserDetails(tokenProvider.getUserId(token), status, authorities);

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                customUserDetails, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("access_token"))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
