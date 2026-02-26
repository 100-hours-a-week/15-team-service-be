package com.sipomeokjo.commitme.domain.chat.service;

import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import com.sipomeokjo.commitme.domain.chat.entity.Chatroom;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatroomRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatroomQueryService {

    private final ChatroomRepository chatroomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatroomResponse> getAllChatroom() {
        List<Chatroom> chatrooms = chatroomRepository.findAllWithPosition();
        List<ChatroomWithLatest> merged =
                chatrooms.stream()
                        .map(
                                chatroom ->
                                        new ChatroomWithLatest(
                                                chatroom, findLatestMessage(chatroom.getId())))
                        .toList();
        return merged.stream().sorted(chatroomLatestComparator()).map(this::toResponse).toList();
    }

    private ChatMessage findLatestMessage(Long chatroomId) {
        List<ChatMessage> latest =
                chatMessageRepository.findLatestByChatroomId(chatroomId, PageRequest.of(0, 1));
        return latest.isEmpty() ? null : latest.getFirst();
    }

    private Comparator<ChatroomWithLatest> chatroomLatestComparator() {
        return Comparator.<ChatroomWithLatest, ChatMessage>comparing(
                        ChatroomWithLatest::latest,
                        Comparator.nullsLast(
                                Comparator.comparing(ChatMessage::getCreatedAt)
                                        .thenComparing(ChatMessage::getId)))
                .reversed();
    }

    private ChatroomResponse toResponse(ChatroomWithLatest merged) {
        Chatroom chatroom = merged.chatroom();
        ChatMessage latest = merged.latest();
        return new ChatroomResponse(
                chatroom.getId(),
                chatroom.getPosition().getName(),
                latest == null ? null : latest.getMessage(),
                latest == null ? null : latest.getCreatedAt());
    }

    private record ChatroomWithLatest(Chatroom chatroom, ChatMessage latest) {}
}
