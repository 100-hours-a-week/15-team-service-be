package com.sipomeokjo.commitme.domain.chat.service;

import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.repository.ChatroomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomQueryService {

    private final ChatroomRepository chatroomRepository;

    public List<ChatroomResponse> getAllChatroom() {
        return chatroomRepository.findAllChatroom();
    }
}
