package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.security.csrf.CsrfTokenResponseCookieFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalSecurityConfig {

    private final CsrfProperties csrfProperties;

    @Bean
    @Order(0)
    public SecurityFilterChain localPermitAllFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (csrfProperties.cookieDomain() != null && !csrfProperties.cookieDomain().isBlank()) {
            csrfTokenRepository.setCookieDomain(csrfProperties.cookieDomain());
        }
        return http.securityMatcher("/**")
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(csrfTokenRepository)
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler())
                                        .ignoringRequestMatchers(
                                                "/api/v1/resume/callback", "/uploads/**"))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .addFilterAfter(
                        new CsrfTokenResponseCookieFilter(csrfTokenRepository), CsrfFilter.class)
                .build();
    }
}
