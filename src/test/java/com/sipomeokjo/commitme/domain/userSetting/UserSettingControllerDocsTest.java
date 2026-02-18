package com.sipomeokjo.commitme.domain.userSetting;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.config.WebMvcConfig;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.userSetting.controller.UserSettingController;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsResponse;
import com.sipomeokjo.commitme.domain.userSetting.dto.UserSettingsUpdateRequest;
import com.sipomeokjo.commitme.domain.userSetting.service.UserSettingCommandService;
import com.sipomeokjo.commitme.domain.userSetting.service.UserSettingQueryService;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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

@WebMvcTest(UserSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(WebMvcConfig.class)
class UserSettingControllerDocsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserSettingQueryService userSettingQueryService;
    @MockitoBean private UserSettingCommandService userSettingCommandService;
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getSettings_docs() throws Exception {
        UserSettingsResponse response = new UserSettingsResponse(1L, true, false);
        given(userSettingQueryService.getSettings(1L)).willReturn(response);

        mockMvc.perform(get("/user/settings").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-settings-get",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("사용자 설정 조회")
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
                                                        fieldWithPath("data.notificationEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("알림 수신 여부"),
                                                        fieldWithPath(
                                                                        "data.interviewResumeDefaultsEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("면접/이력서 기본값 사용 여부"))
                                                .build())));
    }

    @Test
    void updateSettings_docs() throws Exception {
        UserSettingsUpdateRequest request = new UserSettingsUpdateRequest(false, true);
        UserSettingsResponse response = new UserSettingsResponse(1L, false, true);
        given(
                        userSettingCommandService.updateSettings(
                                eq(1L), any(UserSettingsUpdateRequest.class)))
                .willReturn(response);

        mockMvc.perform(
                        patch("/user/settings")
                                .with(authenticatedUser())
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "user-settings-update",
                                resource(
                                        builder()
                                                .tag("User")
                                                .summary("사용자 설정 수정")
                                                .requestFields(
                                                        fieldWithPath("notificationEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("알림 수신 여부"),
                                                        fieldWithPath(
                                                                        "interviewResumeDefaultsEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("면접/이력서 기본값 사용 여부"))
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
                                                        fieldWithPath("data.notificationEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("알림 수신 여부"),
                                                        fieldWithPath(
                                                                        "data.interviewResumeDefaultsEnabled")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("면접/이력서 기본값 사용 여부"))
                                                .build())));
    }

    private RequestPostProcessor authenticatedUser() {
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + UserStatus.ACTIVE.name()));
        CustomUserDetails details = new CustomUserDetails(1L, UserStatus.ACTIVE, authorities);
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
