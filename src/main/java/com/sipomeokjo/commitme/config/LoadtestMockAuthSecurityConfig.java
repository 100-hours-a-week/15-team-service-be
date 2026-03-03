package com.sipomeokjo.commitme.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(LoadtestProperties.class)
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
                .build();
    }
}
