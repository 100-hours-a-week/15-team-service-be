package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeMongoRepository extends MongoRepository<ResumeDocument, String> {

    Optional<ResumeDocument> findByResumeId(Long resumeId);

    Optional<ResumeDocument> findByResumeIdAndUserId(Long resumeId, Long userId);

    Optional<ResumeDocument> findTopByUserIdOrderByUpdatedAtDescResumeIdDesc(Long userId);

    List<ResumeDocument> findByUserIdIn(List<Long> userIds);

    boolean existsByResumeIdAndUserId(Long resumeId, Long userId);

    boolean existsByUserIdAndHasPendingWorkTrue(Long userId);

    void deleteByResumeId(Long resumeId);

    void deleteByUserIdIn(List<Long> userIds);
}
