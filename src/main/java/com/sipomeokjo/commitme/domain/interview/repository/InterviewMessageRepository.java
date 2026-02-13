package com.sipomeokjo.commitme.domain.interview.repository;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {

    @Query("SELECT m FROM InterviewMessage m "
            + "WHERE m.interview.id = :interviewId "
            + "ORDER BY m.turnNo ASC")
    List<InterviewMessage> findAllByInterviewIdOrderByTurnNo(@Param("interviewId") Long interviewId);

    void deleteAllByInterviewId(Long interviewId);
}
