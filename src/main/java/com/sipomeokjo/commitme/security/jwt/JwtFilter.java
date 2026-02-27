package com.sipomeokjo.commitme.security.jwt;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final AccessTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null) {
            log.debug(
                    "[JWT] missing_token method={} uri={}",
                    request.getMethod(),
                    request.getRequestURI());
        }
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
            } else {
                log.warn(
                        "[JWT] invalid_status method={} uri={} status={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        statusValue);
            }
        } else if (token != null) {
            log.warn(
                    "[JWT] invalid_token method={} uri={}",
                    request.getMethod(),
                    request.getRequestURI());
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

        List<String> candidates =
                Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals("access_token"))
                        .map(Cookie::getValue)
                        .filter(value -> !value.isBlank())
                        .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        log.debug(
                "[JWT] duplicated_access_token_cookie method={} uri={} count={}",
                request.getMethod(),
                request.getRequestURI(),
                candidates.size());
        return selectLatestIssuedToken(candidates);
    }

    private String selectLatestIssuedToken(List<String> candidates) {
        String selectedToken = null;
        Instant selectedIssuedAt = null;

        for (String candidate : candidates) {
            if (!tokenProvider.validateToken(candidate)) {
                continue;
            }

            Instant issuedAt = tokenProvider.getIssuedAt(candidate);
            if (issuedAt == null) {
                continue;
            }

            if (selectedToken == null || issuedAt.isAfter(selectedIssuedAt)) {
                selectedToken = candidate;
                selectedIssuedAt = issuedAt;
            }
        }

        return selectedToken != null ? selectedToken : candidates.getFirst();
    }
}
