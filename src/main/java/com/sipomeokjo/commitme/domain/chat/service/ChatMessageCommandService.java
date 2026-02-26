package com.sipomeokjo.commitme.domain.chat.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageResponse;
import com.sipomeokjo.commitme.domain.chat.dto.ChatMessageSendRequest;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachment;
import com.sipomeokjo.commitme.domain.chat.entity.ChatAttachmentType;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageRole;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageStatus;
import com.sipomeokjo.commitme.domain.chat.entity.ChatMessageType;
import com.sipomeokjo.commitme.domain.chat.entity.ChatUserNumber;
import com.sipomeokjo.commitme.domain.chat.entity.Chatroom;
import com.sipomeokjo.commitme.domain.chat.mapper.ChatMessageMapper;
import com.sipomeokjo.commitme.domain.chat.repository.ChatAttachmentRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatUserNumberRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatroomRepository;
import com.sipomeokjo.commitme.domain.upload.entity.Upload;
import com.sipomeokjo.commitme.domain.upload.entity.UploadPurpose;
import com.sipomeokjo.commitme.domain.upload.entity.UploadStatus;
import com.sipomeokjo.commitme.domain.upload.repository.UploadRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageCommandService {

    private final ChatroomRepository chatroomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatAttachmentRepository chatAttachmentRepository;
    private final ChatUserNumberRepository chatUserNumberRepository;
    private final UploadRepository uploadRepository;
    private final S3UploadService s3UploadService;
    private final UserRepository userRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final Clock clock;

    public ChatMessageResponse sendMessage(
            Long chatroomId, Long userId, ChatMessageSendRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Chatroom chatroom =
                chatroomRepository
                        .findById(chatroomId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CHATROOM_NOT_FOUND));

        User sender =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (sender.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        ensureChatUserNumber(chatroom, sender);

        User mentionUser = null;
        if (request.mentionUserId() != null) {
            mentionUser =
                    userRepository
                            .findById(request.mentionUserId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        List<Long> uploadIds = request.attachmentUploadIds();
        List<Upload> uploads =
                uploadIds == null || uploadIds.isEmpty()
                        ? Collections.emptyList()
                        : uploadRepository.findAllById(uploadIds);

        validateUploads(uploads, uploadIds, userId);

        ChatMessageType messageType = resolveMessageType(request.message(), uploads);

        ChatMessage message =
                ChatMessage.builder()
                        .chatroom(chatroom)
                        .sender(sender)
                        .mentionUser(mentionUser)
                        .status(ChatMessageStatus.SENT)
                        .role(ChatMessageRole.CHAT)
                        .messageType(messageType)
                        .message(request.message())
                        .createdAt(Instant.now(clock))
                        .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        List<ChatAttachment> attachments = buildAttachments(savedMessage, uploads);
        if (!attachments.isEmpty()) {
            chatAttachmentRepository.saveAll(attachments);
        }

        Map<Long, List<ChatAttachment>> attachmentsByMessageId =
                attachments.isEmpty()
                        ? Collections.emptyMap()
                        : Map.of(savedMessage.getId(), attachments);

        Map<Long, Integer> userNumbersByUserId = fetchUserNumbers(chatroomId);
        return chatMessageMapper.toChatMessageResponse(
                savedMessage, attachmentsByMessageId, userNumbersByUserId);
    }

    private void validateUploads(List<Upload> uploads, List<Long> uploadIds, Long userId) {
        if (uploadIds == null || uploadIds.isEmpty()) {
            return;
        }
        if (uploads.size() != uploadIds.size()) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_FOUND);
        }
        for (Upload upload : uploads) {
            if (!upload.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.UPLOAD_FORBIDDEN);
            }
            if (upload.getPurpose() != UploadPurpose.CHAT_ATTACHMENT) {
                throw new BusinessException(ErrorCode.UPLOAD_PURPOSE_INVALID);
            }
            if (upload.getStatus() != UploadStatus.UPLOADED) {
                throw new BusinessException(ErrorCode.UPLOAD_STATUS_INVALID);
            }
        }
    }

    private ChatMessageType resolveMessageType(String message, List<Upload> uploads) {
        boolean hasMessage = message != null && !message.isBlank();
        boolean hasUploads = uploads != null && !uploads.isEmpty();
        if (!hasMessage && !hasUploads) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID);
        }
        if (hasMessage && hasUploads) {
            return ChatMessageType.MIXED;
        }
        if (hasUploads) {
            return ChatMessageType.IMAGE;
        }
        return ChatMessageType.TEXT;
    }

    private List<ChatAttachment> buildAttachments(ChatMessage message, List<Upload> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            return Collections.emptyList();
        }
        int order = 1;
        List<ChatAttachment> attachments = new java.util.ArrayList<>();
        for (Upload upload : uploads) {
            if (upload == null) {
                continue;
            }
            attachments.add(
                    ChatAttachment.builder()
                            .message(message)
                            .fileType(ChatAttachmentType.IMAGE)
                            .fileUrl(s3UploadService.toS3Key(upload.getS3Key()))
                            .orderNo(order++)
                            .createdAt(Instant.now(clock))
                            .build());
        }
        return attachments;
    }

    private Map<Long, Integer> fetchUserNumbers(Long chatroomId) {
        List<ChatUserNumber> userNumbers = chatUserNumberRepository.findByChatroomId(chatroomId);
        if (userNumbers == null || userNumbers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (ChatUserNumber userNumber : userNumbers) {
            result.put(userNumber.getUser().getId(), userNumber.getNumber());
        }
        return result;
    }

    private void ensureChatUserNumber(Chatroom chatroom, User user) {
        Long chatroomId = chatroom.getId();
        Long userId = user.getId();
        if (chatUserNumberRepository.existsByChatroomIdAndUserId(chatroomId, userId)) {
            return;
        }
        Integer maxNumber = chatUserNumberRepository.findMaxNumberByChatroomId(chatroomId);
        int nextNumber = (maxNumber == null ? 1 : maxNumber + 1);
        ChatUserNumber newUserNumber =
                ChatUserNumber.builder().chatroom(chatroom).user(user).number(nextNumber).build();
        chatUserNumberRepository.save(newUserNumber);
    }
}
