package com.sipomeokjo.commitme.domain.auth;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sipomeokjo.commitme.config.AuthRedirectProperties;
import com.sipomeokjo.commitme.config.CorsProperties;
import com.sipomeokjo.commitme.config.SecurityConfig;
import com.sipomeokjo.commitme.domain.auth.controller.AuthController;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.auth.service.AuthCommandService;
import com.sipomeokjo.commitme.domain.auth.service.AuthQueryService;
import com.sipomeokjo.commitme.security.CookieProperties;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.jwt.JwtFilter;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import com.sipomeokjo.commitme.security.oauth.AuthLoginAuthenticationProvider;
import com.sipomeokjo.commitme.security.oauth.AuthLoginFailureHandler;
import com.sipomeokjo.commitme.security.oauth.AuthLoginSuccessHandler;
import com.sipomeokjo.commitme.security.oauth.AuthLogoutSuccessHandler;
import com.sipomeokjo.commitme.security.oauth.CustomAccessDeniedHandler;
import com.sipomeokjo.commitme.security.oauth.CustomAuthenticationEntryPoint;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({
    SecurityConfig.class,
    JwtFilter.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    AuthLoginAuthenticationProvider.class,
    AuthLoginSuccessHandler.class,
    AuthLoginFailureHandler.class,
    AuthLogoutSuccessHandler.class,
    AuthControllerDocsTest.TestConfig.class
})
class AuthControllerDocsTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthQueryService authQueryService;
    @MockitoBean private AuthCommandService authCommandService;
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("test-secret");
            properties.setAccessExpiration(Duration.ofHours(1));
            properties.setRefreshExpiration(Duration.ofDays(7));
            return properties;
        }

        @Bean
        CookieProperties cookieProperties() {
            CookieProperties properties = new CookieProperties();
            properties.setSecure(false);
            return properties;
        }

        @Bean
        AuthRedirectProperties authRedirectProperties() {
            return new AuthRedirectProperties("https://example.com/auth/callback");
        }

        @Bean
        CorsProperties corsProperties() {
            return new CorsProperties(
                    List.of("http://localhost:5173"),
                    List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"),
                    List.of("*"),
                    List.of(HttpHeaders.SET_COOKIE),
                    true,
                    3600);
        }
    }

    @Test
    void getLoginUrl_docs() throws Exception {
        given(authQueryService.generateState()).willReturn("state-token");
        given(authQueryService.getLoginUrl("state-token"))
                .willReturn("https://github.com/login/oauth/authorize");

        mockMvc.perform(get("/auth/github/loginUrl"))
                .andExpect(status().isOk())
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.SET_COOKIE,
                                        hasItem(org.hamcrest.Matchers.containsString("state="))))
                .andDo(
                        document(
                                "auth-get-login-url",
                                resource(
                                        builder()
                                                .tag("Auth")
                                                .summary("GitHub 로그인 URL 발급")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.loginUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("GitHub 로그인 URL"))
                                                .build()),
                                responseHeaders(
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("로그인 state 쿠키"))));
    }

    @Test
    void issueAccessToken_docs() throws Exception {
        given(authCommandService.reissueAccessToken(anyString()))
                .willReturn(new AuthTokenReissueResult("new-access-token", "new-refresh-token"));

        mockMvc.perform(
                        post("/auth/token")
                                .cookie(new Cookie("refresh_token", "refresh-token"))
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(
                        header().stringValues(
                                        HttpHeaders.SET_COOKIE,
                                        Matchers.hasItems(
                                                Matchers.containsString("access_token="),
                                                Matchers.containsString("refresh_token="))))
                .andDo(
                        document(
                                "auth-issue-access-token",
                                resource(
                                        builder()
                                                .tag("Auth")
                                                .summary("액세스 토큰 재발급")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data")
                                                                .type(JsonFieldType.NULL)
                                                                .description("응답 데이터"))
                                                .build()),
                                requestCookies(
                                        cookieWithName("refresh_token").description("리프레시 토큰 쿠키")),
                                responseHeaders(
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description(
                                                        "재발급된 access_token, refresh_token 쿠키"))));
    }

    @Test
    void githubCallback_docs() throws Exception {
        AuthLoginResult loginResult = new AuthLoginResult("access", "refresh", true);
        given(authCommandService.loginWithGithub("code-123")).willReturn(loginResult);
        given(accessTokenProvider.getUserId("access")).willReturn(1L);

        mockMvc.perform(
                        get("/auth/github")
                                .param("code", "code-123")
                                .param("state", "state-token")
                                .cookie(new Cookie("state", "state-token")))
                .andExpect(status().isFound())
                .andExpect(
                        header().string(
                                        HttpHeaders.LOCATION,
                                        org.hamcrest.Matchers.containsString("status=success")))
                .andDo(
                        document(
                                "auth-github-callback",
                                resource(
                                        builder()
                                                .tag("Auth")
                                                .summary("GitHub OAuth 콜백")
                                                .description("GitHub OAuth 인증 후 콜백 처리")
                                                .build()),
                                requestCookies(
                                        cookieWithName("state").description("CSRF state 쿠키")),
                                responseHeaders(
                                        headerWithName(HttpHeaders.LOCATION)
                                                .description("프론트엔드 리다이렉트 URL"),
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description(
                                                        "access_token, refresh_token, state 만료 쿠키"))));
    }

    @Test
    void logout_docs() throws Exception {
        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "auth-logout",
                                resource(
                                        builder()
                                                .tag("Auth")
                                                .summary("로그아웃")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data")
                                                                .type(JsonFieldType.NULL)
                                                                .description("응답 데이터"))
                                                .build()),
                                responseHeaders(
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description(
                                                        "access_token, refresh_token 만료 쿠키"))));
    }
}
