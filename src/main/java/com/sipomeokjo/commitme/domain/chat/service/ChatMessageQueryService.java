package com.sipomeokjo.commitme.domain.chat.service;

import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachment;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import com.sipomeokjo.commitme.domain.chat.entity.ChatUserNumber;
import com.sipomeokjo.commitme.domain.chat.mapper.ChatMessageMapper;
import com.sipomeokjo.commitme.domain.chat.repository.ChatAttachmentRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatUserNumberRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatUserNumberRepository chatUserNumberRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final CursorParser cursorParser;

    public CursorResponse<ChatMessageResponse> getChatMessages(Long chatroomId, CursorRequest request) {
        CursorParser.Cursor cursor = cursorParser.parse(request == null ? null : request.next());
        int size = (request == null ? 15 : request.limit(15));

        List<ChatMessage> messages = chatMessageRepository.findByChatroomWithCursor(
                chatroomId,
                cursor.createdAt(),
                cursor.id(),
                PageRequest.of(0, size + 1)
        );

        boolean hasMorePast = messages.size() > size;
        List<ChatMessage> pageDesc = hasMorePast ? messages.subList(0, size) : messages;
        List<ChatMessage> pageAsc = pageDesc.reversed();

        Map<Long, List<ChatAttachment>> attachmentsByMessageId = fetchAttachments(pageAsc);
        Map<Long, Integer> userNumbersByUserId = fetchUserNumbers(chatroomId);
        List<ChatMessageResponse> responses = pageAsc.stream()
                .map(message -> chatMessageMapper.toChatMessageResponse(
                        message,
                        attachmentsByMessageId,
                        userNumbersByUserId
                ))
                .toList();

        String next = hasMorePast && !pageAsc.isEmpty()
                ? encodeCursor(pageAsc.getFirst())
                : null;

        return new CursorResponse<>(responses, null, next);
    }

    private Map<Long, List<ChatAttachment>> fetchAttachments(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getId)
                .toList();
        return chatAttachmentRepository.findByMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(attachment -> attachment.getMessage().getId()));
    }

    private Map<Long, Integer> fetchUserNumbers(Long chatroomId) {
        return chatUserNumberRepository.findByChatroomId(chatroomId).stream()
                .collect(Collectors.toMap(
                        chatUserNumber -> chatUserNumber.getUser().getId(),
                        ChatUserNumber::getNumber,
                        (existing, replacement) -> existing
                ));
    }

    private String encodeCursor(ChatMessage message) {
        return message.getCreatedAt().toString() + "|" + message.getId();
    }

}
