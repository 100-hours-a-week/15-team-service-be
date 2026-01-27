package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.entity.ChatUserNumber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatUserNumberRepository extends JpaRepository<ChatUserNumber, Long> {

    List<ChatUserNumber> findByChatroomId(Long chatroomId);
}
