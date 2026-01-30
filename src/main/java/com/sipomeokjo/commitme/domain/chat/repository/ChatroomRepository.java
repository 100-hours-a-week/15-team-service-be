package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse;
import com.sipomeokjo.commitme.domain.chat.entity.Chatroom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    @Query(
            """
        select new com.sipomeokjo.commitme.domain.chat.dto.ChatroomResponse(
            chatRoom.id,
            chatRoom.position.name,
            chatMessage.message,
            chatMessage.createdAt
        )
        from Chatroom chatRoom
        left join ChatMessage chatMessage
            on chatMessage.chatroom = chatRoom
            and chatMessage.id = (
                select max(message.id)
                from ChatMessage message
                where message.chatroom = chatRoom
                    and message.createdAt = (
                        select max(latestMessage.createdAt)
                        from ChatMessage latestMessage
                        where latestMessage.chatroom = chatRoom
                    )
            )
        order by chatMessage.createdAt desc, chatMessage.id desc
        """)
    List<ChatroomResponse> findAllChatroom();
}
