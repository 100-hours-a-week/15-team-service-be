package com.sipomeokjo.commitme.domain.resume;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.config.WebMvcConfig;
import com.sipomeokjo.commitme.domain.resume.controller.ResumeController;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProfileService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.support.TestSupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumeController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(WebMvcConfig.class)
class ResumeControllerDocsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ResumeService resumeService;
    @MockitoBean private ResumeProfileService resumeProfileService;
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getResume_docs() throws Exception {
        ResumeDetailDto response =
                new ResumeDetailDto(
                        1L,
                        "이력서 제목",
                        false,
                        2L,
                        "백엔드",
                        3L,
                        "커밋미",
                        4,
                        "{\"summary\":\"내용\"}",
                        new ResumeDetailDto.ResumeDetailProfileDto(
                                "홍길동",
                                "https://cdn.commit-me.com/profile.png",
                                "+82",
                                "1012345678",
                                "소개",
                                List.of(
                                        new ResumeProfileResponse.TechStackResponse(
                                                1L, "Spring Boot")),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of()),
                        Instant.parse("2026-03-16T04:00:00Z"),
                        Instant.parse("2026-03-15T04:00:00Z"),
                        Instant.parse("2026-03-16T05:00:00Z"));
        given(resumeService.get(1L, 1L)).willReturn(response);

        mockMvc.perform(get("/resumes/{resumeId}", 1L).with(TestSupport.testAuthenticatedUser()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "resume-get",
                                resource(
                                        builder()
                                                .tag("Resume")
                                                .summary("이력서 상세 조회")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.resumeId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("이력서 ID"),
                                                        fieldWithPath("data.name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이력서명"),
                                                        fieldWithPath("data.isEditing")
                                                                .type(JsonFieldType.BOOLEAN)
                                                                .description("AI 수정 진행 여부"),
                                                        fieldWithPath("data.positionId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("직무 ID"),
                                                        fieldWithPath("data.positionName")
                                                                .type(JsonFieldType.STRING)
                                                                .description("직무명"),
                                                        fieldWithPath("data.companyId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .optional()
                                                                .description("회사 ID"),
                                                        fieldWithPath("data.companyName")
                                                                .type(JsonFieldType.STRING)
                                                                .optional()
                                                                .description("회사명"),
                                                        fieldWithPath("data.currentVersionNo")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("현재 버전 번호"),
                                                        fieldWithPath("data.content")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이력서 내용 JSON 문자열"),
                                                        fieldWithPath("data.profile")
                                                                .type(JsonFieldType.OBJECT)
                                                                .description("이력서 생성 시 사용한 개인정보"),
                                                        fieldWithPath("data.profile.name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이름"),
                                                        fieldWithPath(
                                                                        "data.profile.profileImageUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("프로필 이미지 URL")
                                                                .optional(),
                                                        fieldWithPath(
                                                                        "data.profile.phoneCountryCode")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화 국가번호")
                                                                .optional(),
                                                        fieldWithPath("data.profile.phoneNumber")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전화번호")
                                                                .optional(),
                                                        fieldWithPath("data.profile.introduction")
                                                                .type(JsonFieldType.STRING)
                                                                .description("자기소개")
                                                                .optional(),
                                                        subsectionWithPath(
                                                                        "data.profile.techStacks")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("기술 스택 목록"),
                                                        subsectionWithPath(
                                                                        "data.profile.experiences")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("경력 목록"),
                                                        subsectionWithPath(
                                                                        "data.profile.educations")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("학력 목록"),
                                                        subsectionWithPath(
                                                                        "data.profile.activities")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("활동 목록"),
                                                        subsectionWithPath(
                                                                        "data.profile.certificates")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("자격증 목록"),
                                                        fieldWithPath("data.commitedAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("버전 확정 시각")
                                                                .optional(),
                                                        fieldWithPath("data.createdAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이력서 생성 시각"),
                                                        fieldWithPath("data.updatedAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이력서 수정 시각"))
                                                .build())));
    }

    @Test
    void editResume_docs() throws Exception {
        ResumeEditResponse response =
                new ResumeEditResponse(
                        1L, 2, "이력서 제목", "task-1234", Instant.parse("2026-02-17T10:00:00Z"));
        given(resumeService.edit(eq(1L), eq(1L), any(ResumeEditRequest.class)))
                .willReturn(response);

        String requestBody = objectMapper.writeValueAsString(new EditRequestBody("수정 요청"));

        mockMvc.perform(
                        patch("/resumes/{resumeId}", 1L)
                                .with(TestSupport.testAuthenticatedUser())
                                .with(csrf())
                                .contentType("application/json")
                                .content(requestBody))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "resume-edit",
                                resource(
                                        builder()
                                                .tag("Resume")
                                                .summary("이력서 수정 요청")
                                                .requestFields(
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("수정 요청 메시지"))
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.resumeId")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("이력서 ID"),
                                                        fieldWithPath("data.versionNo")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("버전 번호"),
                                                        fieldWithPath("data.name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이력서명"),
                                                        fieldWithPath("data.taskId")
                                                                .type(JsonFieldType.STRING)
                                                                .description("AI 작업 ID"),
                                                        fieldWithPath("data.updatedAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("요청 시각"))
                                                .build())));
    }

    private record EditRequestBody(String message) {}
}
