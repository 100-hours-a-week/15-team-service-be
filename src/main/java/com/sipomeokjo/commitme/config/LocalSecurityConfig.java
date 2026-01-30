package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.security.csrf.CsrfTokenResponseCookieFilter;
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
public class LocalSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain localPermitAllFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/**")
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(
                                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler())
                                        .ignoringRequestMatchers("/api/v1/resume/callback"))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .addFilterAfter(new CsrfTokenResponseCookieFilter(), CsrfFilter.class)
                .build();
    }
}
