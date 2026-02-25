package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.security.CookieProperties;
import com.sipomeokjo.commitme.security.CryptoProperties;
import com.sipomeokjo.commitme.security.csrf.CsrfTokenResponseCookieFilter;
import com.sipomeokjo.commitme.security.csrf.LoggingCsrfTokenRepository;
import com.sipomeokjo.commitme.security.jwt.JwtFilter;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import com.sipomeokjo.commitme.security.oauth.AuthLoginAuthenticationFilter;
import com.sipomeokjo.commitme.security.oauth.AuthLoginAuthenticationProvider;
import com.sipomeokjo.commitme.security.oauth.AuthLoginFailureHandler;
import com.sipomeokjo.commitme.security.oauth.AuthLoginSuccessHandler;
import com.sipomeokjo.commitme.security.oauth.AuthLogoutSuccessHandler;
import com.sipomeokjo.commitme.security.oauth.CustomAccessDeniedHandler;
import com.sipomeokjo.commitme.security.oauth.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({
    JwtProperties.class,
    CookieProperties.class,
    CryptoProperties.class,
    CorsProperties.class,
    AuthRedirectProperties.class,
    CsrfProperties.class
})
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final AuthLoginAuthenticationProvider authLoginAuthenticationProvider;
    private final AuthLoginSuccessHandler authLoginSuccessHandler;
    private final AuthLoginFailureHandler authLoginFailureHandler;
    private final AuthLogoutSuccessHandler authLogoutSuccessHandler;
    private final CsrfProperties csrfProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthLoginAuthenticationFilter authLoginFilter = new AuthLoginAuthenticationFilter();
        authLoginFilter.setAuthenticationManager(authLoginAuthenticationManager());
        authLoginFilter.setAuthenticationSuccessHandler(authLoginSuccessHandler);
        authLoginFilter.setAuthenticationFailureHandler(authLoginFailureHandler);
        CookieCsrfTokenRepository baseCsrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        baseCsrfTokenRepository.setCookieCustomizer(
                builder -> {
                    if (csrfProperties.cookieDomain() != null
                            && !csrfProperties.cookieDomain().isBlank()) {
                        builder.domain(csrfProperties.cookieDomain());
                    }
                });
        LoggingCsrfTokenRepository csrfTokenRepository =
                new LoggingCsrfTokenRepository(baseCsrfTokenRepository);

        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(csrfTokenRepository)
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler())
                                        .ignoringRequestMatchers(
                                                "/api/v1/resume/callback", "/uploads/**"))
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.GET, "/positions")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/auth/token")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/uploads/**")
                                        .hasAnyRole("PENDING", "ACTIVE", "INACTIVE")
                                        .requestMatchers(HttpMethod.PATCH, "/uploads/**")
                                        .hasAnyRole("PENDING", "ACTIVE", "INACTIVE")
                                        .requestMatchers(HttpMethod.POST, "/auth/logout")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/auth/github/loginUrl",
                                                "/auth/github",
                                                "/auth/token",
                                                "/api/v1/resume/callback",
                                                "/actuator/health",
                                                "/actuator/prometheus", // internal scrape path
                                                // (SG/ALB 제한 전제)
                                                "/swagger/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/user/onboarding")
                                        .hasAnyRole("PENDING", "INACTIVE")
                                        .anyRequest()
                                        .hasRole("ACTIVE"))
                .logout(
                        logout ->
                                logout.logoutUrl("/auth/logout")
                                        .logoutSuccessHandler(authLogoutSuccessHandler)
                                        .permitAll())
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterAfter(
                        new CsrfTokenResponseCookieFilter(csrfTokenRepository), CsrfFilter.class)
                .addFilterBefore(authLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authLoginAuthenticationManager() {
        return new ProviderManager(authLoginAuthenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());
        configuration.setAllowedHeaders(corsProperties.allowedHeaders());
        configuration.setExposedHeaders(corsProperties.exposedHeaders());
        configuration.setAllowCredentials(corsProperties.allowCredentials());
        configuration.setMaxAge(corsProperties.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
