package com.sipomeokjo.commitme.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class LoadtestMockAuthSecurityConfig {

    @Bean
    @Order(-100)
    @ConditionalOnProperty(
            prefix = "app.loadtest.mock-auth",
            name = "enabled",
            havingValue = "true")
    public SecurityFilterChain loadtestMockAuthSecurityFilterChain(HttpSecurity http)
            throws Exception {
        return http.securityMatcher("/internal/loadtest/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 운영 오노출 방지를 위해 기본 비활성화 + enabled 프로퍼티/인프라 접근제어 전제
                .build();
    }
}
