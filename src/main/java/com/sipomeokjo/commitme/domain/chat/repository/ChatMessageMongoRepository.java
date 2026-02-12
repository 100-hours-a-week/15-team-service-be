package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.document.ChatMessageDocument;
import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

    @Query(
            value =
                    "{ 'chatroomId': ?0, $or: [ "
                            + "{ 'createdAt': { $lt: ?1 } }, "
                            + "{ 'createdAt': ?1, '_id': { $lt: ?2 } } "
                            + "] }",
            sort = "{ 'createdAt': -1, '_id': -1 }")
    List<ChatMessageDocument> findByChatroomWithCursor(
            Long chatroomId, Instant cursorCreatedAt, ObjectId cursorId, Pageable pageable);

    @Query(value = "{ 'chatroomId': ?0 }", sort = "{ 'createdAt': -1, '_id': -1 }")
    List<ChatMessageDocument> findByChatroomIdOrderByCreatedAtDesc(
            Long chatroomId, Pageable pageable);

    @Query(value = "{ 'chatroomId': ?0 }", sort = "{ 'createdAt': -1, '_id': -1 }")
    List<ChatMessageDocument> findLatestByChatroomId(Long chatroomId, Pageable pageable);

    long countByChatroomId(Long chatroomId);
}
