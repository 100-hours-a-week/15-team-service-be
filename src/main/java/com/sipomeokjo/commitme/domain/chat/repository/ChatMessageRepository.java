package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.entity.ChatMessage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Deprecated
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query(
            """
        select message
        from ChatMessage message
        where message.chatroom.id = :chatroomId
            and (
                :cursorCreatedAt is null
                or message.createdAt < :cursorCreatedAt
                or (message.createdAt = :cursorCreatedAt and message.id < :cursorId)
            )
        order by message.createdAt desc, message.id desc
        """)
    List<ChatMessage> findByChatroomWithCursor(
            @Param("chatroomId") Long chatroomId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(
            """
        select message
        from ChatMessage message
        where message.chatroom.id = :chatroomId
        order by message.createdAt desc, message.id desc
        """)
    List<ChatMessage> findLatestByChatroomId(
            @Param("chatroomId") Long chatroomId, Pageable pageable);

    long countByChatroomId(@Param("chatroomId") Long chatroomId);
}
