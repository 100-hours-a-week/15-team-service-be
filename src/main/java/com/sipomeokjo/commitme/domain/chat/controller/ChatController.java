package com.sipomeokjo.commitme.domain.chat.controller;

import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.service.ChatMessageQueryService;
import com.sipomeokjo.commitme.domain.chat.service.ChatroomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatroomQueryService chatroomQueryService;
    private final ChatMessageQueryService chatMessageQueryService;
	
	@GetMapping
	public ResponseEntity<APIResponse<List<ChatroomResponse>>> getChatRoom() {
		return APIResponse.onSuccess(SuccessCode.CHATROOM_FETCHED,
				chatroomQueryService.getAllChatroom());
	}

    @GetMapping("/{chatroomId}")
    public ResponseEntity<APIResponse<CursorResponse<ChatMessageResponse>>> getChatMessages(
            @PathVariable Long chatroomId,
            CursorRequest request) {
        return APIResponse.onSuccess(SuccessCode.CHAT_HISTORY_FETCHED,
                chatMessageQueryService.getChatMessages(chatroomId, request));
    }
}
