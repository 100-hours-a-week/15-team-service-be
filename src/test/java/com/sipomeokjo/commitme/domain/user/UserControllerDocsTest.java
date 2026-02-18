package com.sipomeokjo.commitme.domain.user;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.config.WebMvcConfig;
import com.sipomeokjo.commitme.domain.user.controller.UserController;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserProfileResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.service.UserCommandService;
import com.sipomeokjo.commitme.domain.user.service.UserQueryService;
import com.sipomeokjo.commitme.security.CookieProperties;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import java.time.Duration;
import java.util.List;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({WebMvcConfig.class, UserControllerDocsTest.TestConfig.class})
class UserControllerDocsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserQueryService userQueryService;
    @MockitoBean private UserCommandService userCommandService;
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
    }

    @Test
    void getProfile_docs() throws Exception {
        UserProfileResponse response =
                new UserProfileResponse(1L, "profile-url", "홍길동", 2L, "01012345678", true, false);
        given(userQueryService.getUserProfile(1L)).willReturn(response);

        mockMvc.perform(get("/user").with(authenticatedUser(UserStatus.ACTIVE)))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-get-profile",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("내 정보 조회")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("사용자 ID"),
                                                        fieldWithPath("data.profileImageUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("프로필 이미지 URL"),
                                                        fieldWithPath("data.name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이름"),
                                                        fieldWithPath("data.positionId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("포지션 ID"),
                                                        fieldWithPath("data.phone")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화번호"),
                                                        fieldWithPath("data.privacyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("개인정보 동의"),
                                                        fieldWithPath("data.phonePolicyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("전화번호 개인정보 동의"))
                                                .build())));
    }

    @Test
    void updateProfile_docs() throws Exception {
        UserUpdateRequest request =
                new UserUpdateRequest("profile-url", "홍길동", 2L, "01012345678", true, true);
        UserUpdateResponse response =
                new UserUpdateResponse(
                        1L, "profile-url", "홍길동", 2L, "01012345678", UserStatus.ACTIVE);
        given(userCommandService.updateProfile(eq(1L), any(UserUpdateRequest.class)))
                .willReturn(response);

        mockMvc.perform(
                        patch("/user")
                                .with(authenticatedUser(UserStatus.ACTIVE))
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-update-profile",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("회원 정보 수정")
                                                .requestFields(
                                                        fieldWithPath("profileImageUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("프로필 이미지 URL"),
                                                        fieldWithPath("name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이름"),
                                                        fieldWithPath("positionId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("포지션 ID"),
                                                        fieldWithPath("phone")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화번호"),
                                                        fieldWithPath("privacyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("개인정보 동의"),
                                                        fieldWithPath("phonePolicyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("전화번호 개인정보 동의"))
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("사용자 ID"),
                                                        fieldWithPath("data.profileImageUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("프로필 이미지 URL"),
                                                        fieldWithPath("data.name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이름"),
                                                        fieldWithPath("data.positionId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("포지션 ID"),
                                                        fieldWithPath("data.phone")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화번호"),
                                                        fieldWithPath("data.status")
                                                                .type(JsonFieldType.STRING)
                                                                .description("회원 상태"))
                                                .build())));
    }

    @Test
    void onboarding_docs() throws Exception {
        OnboardingRequest request =
                new OnboardingRequest("profile-url", "홍길동", 2L, "01012345678", true, true);
        OnboardingResponse response = new OnboardingResponse(1L, UserStatus.ACTIVE);
        given(userCommandService.onboard(eq(1L), any(OnboardingRequest.class)))
                .willReturn(response);
        given(accessTokenProvider.createAccessToken(1L, UserStatus.ACTIVE))
                .willReturn("access-token");

        mockMvc.perform(
                        post("/user/onboarding")
                                .with(authenticatedUser(UserStatus.PENDING))
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-onboarding",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("회원가입 완료(온보딩)")
                                                .requestFields(
                                                        fieldWithPath("profileImageUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("프로필 이미지 URL"),
                                                        fieldWithPath("name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이름"),
                                                        fieldWithPath("positionId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("포지션 ID"),
                                                        fieldWithPath("phone")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화번호"),
                                                        fieldWithPath("privacyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("개인정보 동의"),
                                                        fieldWithPath("phonePolicyAgreed")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("전화번호 개인정보 동의"))
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.userId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("사용자 ID"),
                                                        fieldWithPath("data.status")
                                                                .type(JsonFieldType.STRING)
                                                                .description("회원 상태"))
                                                .build()),
                                responseHeaders(
                                        headerWithName(HttpHeaders.SET_COOKIE)
                                                .description("access_token 쿠키"))));
    }

    @Test
    void withdraw_docs() throws Exception {
        mockMvc.perform(delete("/user").with(authenticatedUser(UserStatus.ACTIVE)).with(csrf()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-withdraw",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("회원탈퇴")
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
                                                .description("access_token/refresh_token 만료 쿠키"))));
    }

    private RequestPostProcessor authenticatedUser(UserStatus status) {
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + status.name()));
        CustomUserDetails details = new CustomUserDetails(1L, status, authorities);
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    new UsernamePasswordAuthenticationToken(details, null, authorities));
            SecurityContextHolder.setContext(context);
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return request;
        };
    }
}
