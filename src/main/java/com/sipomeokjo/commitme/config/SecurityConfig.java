package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class, CryptoProperties.class})
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final AuthLoginAuthenticationProvider authLoginAuthenticationProvider;
    private final AuthLoginSuccessHandler authLoginSuccessHandler;
    private final AuthLoginFailureHandler authLoginFailureHandler;
    private final AuthLogoutSuccessHandler authLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthLoginAuthenticationFilter authLoginFilter = new AuthLoginAuthenticationFilter();
        authLoginFilter.setAuthenticationManager(authLoginAuthenticationManager());
        authLoginFilter.setAuthenticationSuccessHandler(authLoginSuccessHandler);
        authLoginFilter.setAuthenticationFailureHandler(authLoginFailureHandler);

        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 API
                        .requestMatchers(HttpMethod.GET, "/auth/token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/github/loginUrl",
                                "/auth/github",
                                "/auth/github/callback",
                                "/auth/token",
                                "/actuator/health",
                                "/swagger/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 🔥 개발 중 임시 허용 (로그인만 되어 있으면 OK)
                        .requestMatchers("/repositories/**").authenticated()

                        // 온보딩
                        .requestMatchers(HttpMethod.POST, "/user/onboarding").hasRole("PENDING")

                        // 그 외는 ACTIVE만
                        .anyRequest().hasRole("ACTIVE")
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler(authLogoutSuccessHandler)
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                // 필터 순서 중요: 로그인 필터 → (그 외 요청은 jwtFilter)
                .addFilterBefore(authLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authLoginAuthenticationManager() {
        return new ProviderManager(authLoginAuthenticationProvider);
    }
}
