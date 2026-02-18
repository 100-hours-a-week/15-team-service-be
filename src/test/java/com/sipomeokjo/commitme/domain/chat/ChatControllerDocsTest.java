package com.sipomeokjo.commitme.domain.chat;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.domain.chat.controller.ChatController;
import com.sipomeokjo.commitme.domain.chat.dto.ChatAttachmentResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.service.ChatMessageQueryService;
import com.sipomeokjo.commitme.domain.chat.service.ChatroomQueryService;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.security.handler.CustomUserDetails;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class ChatControllerDocsTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ChatroomQueryService chatroomQueryService;
    @MockitoBean private ChatMessageQueryService chatMessageQueryService;
    @MockitoBean private AccessTokenProvider accessTokenProvider;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void getChatRooms_docs() throws Exception {
        List<ChatroomResponse> responses =
                List.of(
                        new ChatroomResponse(
                                1L, "채팅방1", "마지막 메시지", Instant.parse("2026-01-30T06:00:00Z")));
        given(chatroomQueryService.getAllChatroom()).willReturn(responses);

        mockMvc.perform(get("/chats").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "chat-get-rooms",
                                resource(
                                        builder()
                                                .tag("Chat")
                                                .summary("채팅방 목록 조회")
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data[]")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("채팅방 목록"),
                                                        fieldWithPath("data[].id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("채팅방 ID"),
                                                        fieldWithPath("data[].name")
                                                                .type(JsonFieldType.STRING)
                                                                .description("채팅방 이름"),
                                                        fieldWithPath("data[].lastMessage")
                                                                .type(JsonFieldType.STRING)
                                                                .description("마지막 메시지"),
                                                        fieldWithPath("data[].lastUpdatedAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("마지막 업데이트 시각"))
                                                .build())));
    }

    @Test
    void getChatMessages_docs() throws Exception {
        List<ChatAttachmentResponse> files =
                List.of(new ChatAttachmentResponse(10L, "file-url", "IMAGE"));
        List<ChatMessageResponse> messages =
                List.of(
                        new ChatMessageResponse(
                                100L,
                                ChatMessageRole.CHAT,
                                "안녕하세요",
                                files,
                                ChatMessageStatus.SENT,
                                1L,
                                1L,
                                null,
                                null,
                                Instant.parse("2026-01-30T06:10:00Z")));
        CursorResponse<ChatMessageResponse> response =
                new CursorResponse<>(messages, null, "next-cursor");
        given(chatMessageQueryService.getChatMessages(eq(1L), any(CursorRequest.class)))
                .willReturn(response);

        mockMvc.perform(
                        get("/chats/{chatroomId}", 1L)
                                .param("next", "")
                                .param("size", "20")
                                .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "chat-get-messages",
                                resource(
                                        builder()
                                                .tag("Chat")
                                                .summary("채팅 메시지 목록 조회")
                                                .queryParameters(
                                                        parameterWithName("next")
                                                                .description("다음 커서")
                                                                .optional(),
                                                        parameterWithName("size")
                                                                .description("조회 개수")
                                                                .optional())
                                                .responseFields(
                                                        fieldWithPath("code")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 코드"),
                                                        fieldWithPath("message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("응답 메시지"),
                                                        fieldWithPath("data.data")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("채팅 메시지 목록"),
                                                        fieldWithPath("data.data[].id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("메시지 ID"),
                                                        fieldWithPath("data.data[].role")
                                                                .type(JsonFieldType.STRING)
                                                                .description("메시지 역할"),
                                                        fieldWithPath("data.data[].message")
                                                                .type(JsonFieldType.STRING)
                                                                .description("메시지"),
                                                        fieldWithPath("data.data[].files")
                                                                .type(JsonFieldType.ARRAY)
                                                                .description("첨부 파일 목록"),
                                                        fieldWithPath("data.data[].files[].id")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("첨부 ID"),
                                                        fieldWithPath("data.data[].files[].fileUrl")
                                                                .type(JsonFieldType.STRING)
                                                                .description("파일 URL"),
                                                        fieldWithPath(
                                                                        "data.data[].files[].fileType")
                                                                .type(JsonFieldType.STRING)
                                                                .description("파일 타입"),
                                                        fieldWithPath("data.data[].status")
                                                                .type(JsonFieldType.STRING)
                                                                .description("메시지 상태"),
                                                        fieldWithPath("data.data[].sender")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("발신자 ID"),
                                                        fieldWithPath("data.data[].senderNumber")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("발신자 번호"),
                                                        fieldWithPath("data.data[].mentionTo")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("멘션 대상 ID")
                                                                .optional(),
                                                        fieldWithPath("data.data[].mentionToNumber")
                                                                .type(JsonFieldType.NUMBER)
                                                                .description("멘션 대상 번호")
                                                                .optional(),
                                                        fieldWithPath("data.data[].sendAt")
                                                                .type(JsonFieldType.STRING)
                                                                .description("전송 시각"),
                                                        fieldWithPath("data.before")
                                                                .type(JsonFieldType.STRING)
                                                                .description("이전 커서")
                                                                .optional(),
                                                        fieldWithPath("data.next")
                                                                .type(JsonFieldType.STRING)
                                                                .description("다음 커서")
                                                                .optional())
                                                .build())));
    }

    private RequestPostProcessor authenticatedUser() {
        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + UserStatus.ACTIVE.name()));
        CustomUserDetails details = new CustomUserDetails(1L, UserStatus.ACTIVE, authorities);
        return authentication(new UsernamePasswordAuthenticationToken(details, null, authorities));
    }
}
