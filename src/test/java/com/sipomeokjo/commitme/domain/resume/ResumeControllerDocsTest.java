package com.sipomeokjo.commitme.domain.resume;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.config.WebMvcConfig;
import com.sipomeokjo.commitme.domain.resume.controller.ResumeController;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditResponse;
import com.sipomeokjo.commitme.domain.resume.service.ResumeService;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.support.TestSupport;
import java.time.Instant;
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
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

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
