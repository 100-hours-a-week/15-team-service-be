package com.sipomeokjo.commitme.domain.chat.repository;

import com.sipomeokjo.commitme.domain.chat.entity.Chatroom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

    @Query(
            """
        select chatRoom
        from Chatroom chatRoom
        join fetch chatRoom.position
        """)
    List<Chatroom> findAllWithPosition();
}
