package com.sipomeokjo.commitme.domain.interview.repository;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InterviewMessageRepository extends MongoRepository<InterviewMessage, String> {

    List<InterviewMessage> findByInterviewIdOrderByTurnNoAsc(Long interviewId);

    void deleteByInterviewId(Long interviewId);

    Optional<InterviewMessage> findByInterviewIdAndTurnNo(Long interviewId, Integer turnNo);
}
