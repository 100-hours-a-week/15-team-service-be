package com.sipomeokjo.commitme.domain.chat.controller;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.service.ChatroomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatroomQueryService chatroomQueryService;
	
	@GetMapping
	public ResponseEntity<APIResponse<List<ChatroomResponse>>> getChatRoom() {
		return APIResponse.onSuccess(SuccessCode.CHATROOM_FETCHED,
				chatroomQueryService.getAllChatroom());
	}
}
