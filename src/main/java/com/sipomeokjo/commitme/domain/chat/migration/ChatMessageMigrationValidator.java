package com.sipomeokjo.commitme.domain.chat.migration;

import com.sipomeokjo.commitme.domain.chat.entity.Chatroom;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageMongoRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatMessageRepository;
import com.sipomeokjo.commitme.domain.chat.repository.ChatroomRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageMigrationValidator {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMongoRepository chatMessageMongoRepository;
    private final ChatroomRepository chatroomRepository;

    @Transactional(readOnly = true)
    public ValidationResult validate() {
        log.info("Starting migration validation");

        long mysqlTotalCount = chatMessageRepository.count();
        long mongoTotalCount = chatMessageMongoRepository.count();

        log.info("Total count - MySQL: {}, MongoDB: {}", mysqlTotalCount, mongoTotalCount);

        List<ChatroomValidation> chatroomValidations = new ArrayList<>();
        List<Chatroom> chatrooms = chatroomRepository.findAll();

        for (Chatroom chatroom : chatrooms) {
            Long chatroomId = chatroom.getId();
            long mysqlCount = chatMessageRepository.countByChatroomId(chatroomId);
            long mongoCount = chatMessageMongoRepository.countByChatroomId(chatroomId);

            ChatroomValidation validation =
                    new ChatroomValidation(chatroomId, mysqlCount, mongoCount, mysqlCount == mongoCount);

            chatroomValidations.add(validation);

            if (!validation.isValid()) {
                log.warn(
                        "Mismatch for chatroom {}: MySQL={}, MongoDB={}",
                        chatroomId,
                        mysqlCount,
                        mongoCount);
            }
        }

        boolean isValid =
                mysqlTotalCount == mongoTotalCount
                        && chatroomValidations.stream().allMatch(ChatroomValidation::isValid);

        log.info("Validation completed. Valid: {}", isValid);

        return new ValidationResult(
                mysqlTotalCount, mongoTotalCount, chatroomValidations, isValid);
    }

    public record ValidationResult(
            long mysqlTotalCount,
            long mongoTotalCount,
            List<ChatroomValidation> chatroomValidations,
            boolean isValid) {}

    public record ChatroomValidation(
            Long chatroomId, long mysqlCount, long mongoCount, boolean isValid) {}
}
