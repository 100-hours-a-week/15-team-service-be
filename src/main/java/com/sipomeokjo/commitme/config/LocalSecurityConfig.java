package com.sipomeokjo.commitme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalSecurityConfig {

    /** local 프로필에서만: 모든 요청 인증 없이 허용 (기존 보안 설정/필터가 있어도, 이 체인이 먼저 잡아먹게 우선순위를 최상으로 둠) */
    @Bean
    @Order(0)
    public SecurityFilterChain localPermitAllFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
