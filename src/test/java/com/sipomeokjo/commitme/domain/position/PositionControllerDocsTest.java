package com.sipomeokjo.commitme.domain.position;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sipomeokjo.commitme.domain.position.controller.PositionController;
import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.service.PositionQueryService;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PositionController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class PositionControllerDocsTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PositionQueryService positionQueryService;
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getPositions_docs() throws Exception {
        List<PositionResponse> responses =
                List.of(new PositionResponse(1L, "Backend"), new PositionResponse(2L, "Frontend"));
        given(positionQueryService.getPositions()).willReturn(responses);

        mockMvc.perform(get("/positions"))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "position-get",
                                resource(
                                        builder()
                                                .tag("Position")
                                                .summary("포지션 목록 조회")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data[]")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("포지션 목록"),
                                                        fieldWithPath("data[].id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("포지션 ID"),
                                                        fieldWithPath("data[].name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("포지션명"))
                                                .build())));
    }
}
