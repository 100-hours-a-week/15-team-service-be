package com.sipomeokjo.commitme.domain.chat.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.entity.ChatUserNumber;
import com.sipomeokjo.commitme.domain.chat.mapper.ChatMessageMongoMapper;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageMongoRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatUserNumberRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService {

    private final ChatMessageMongoRepository chatMessageMongoRepository;
    private final ChatUserNumberRepository chatUserNumberRepository;
    private final ChatMessageMongoMapper chatMessageMongoMapper;

    public CursorResponse<ChatMessageResponse> getChatMessages(
            Long chatroomId, CursorRequest request) {
        MongoCursor cursor = parseCursor(request == null ? null : request.next());
        int size = CursorRequest.resolveLimit(request, 15);

        List<ChatMessageDocument> documents;
        if (cursor.createdAt() == null) {
            documents =
                    chatMessageMongoRepository.findByChatroomIdOrderByCreatedAtDesc(
                            chatroomId, PageRequest.of(0, size + 1));
        } else {
            documents =
                    chatMessageMongoRepository.findByChatroomWithCursor(
                            chatroomId, cursor.createdAt(), cursor.id(), PageRequest.of(0, size + 1));
        }

        boolean hasMorePast = documents.size() > size;
        List<ChatMessageDocument> pageDesc = hasMorePast ? documents.subList(0, size) : documents;
        List<ChatMessageDocument> pageAsc = pageDesc.reversed();

        Map<Long, Integer> userNumbersByUserId = fetchUserNumbers(chatroomId);
        List<ChatMessageResponse> responses =
                pageAsc.stream()
                        .map(doc -> chatMessageMongoMapper.toChatMessageResponse(doc, userNumbersByUserId))
                        .toList();

        String next = hasMorePast && !pageAsc.isEmpty() ? encodeCursor(pageAsc.getFirst()) : null;

        return new CursorResponse<>(responses, null, next);
    }

    private MongoCursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new MongoCursor(null, null);
        }
        String[] parts = cursor.split("\\|", -1);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_VALUE);
        }
        try {
            Instant createdAt = Instant.parse(parts[0]);
            ObjectId id = new ObjectId(parts[1]);
            return new MongoCursor(createdAt, id);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_VALUE);
        }
    }

    private record MongoCursor(Instant createdAt, ObjectId id) {}

    private Map<Long, Integer> fetchUserNumbers(Long chatroomId) {
        return chatUserNumberRepository.findByChatroomId(chatroomId).stream()
                .collect(
                        Collectors.toMap(
                                chatUserNumber -> chatUserNumber.getUser().getId(),
                                ChatUserNumber::getNumber,
                                (existing, replacement) -> existing));
    }

    private String encodeCursor(ChatMessageDocument document) {
        return document.getCreatedAt().toString() + "|" + document.getId();
    }
}
