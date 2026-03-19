package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeMongoRepository extends MongoRepository<ResumeDocument, String> {

    Optional<ResumeDocument> findByResumeId(Long resumeId);

    void deleteByResumeId(Long resumeId);
}
