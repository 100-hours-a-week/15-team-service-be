package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.entity.ChatUserNumber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatUserNumberRepository extends JpaRepository<ChatUserNumber, Long> {

    List<ChatUserNumber> findByChatroomId(Long chatroomId);

    boolean existsByChatroomIdAndUserId(Long chatroomId, Long userId);

    @Query(
            "select max(userNumber.number) from ChatUserNumber userNumber where userNumber.chatroom.id = :chatroomId")
    Integer findMaxNumberByChatroomId(
            @Param("chatroomId") Long chatroomId);
}
