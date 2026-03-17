package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeEventMongoRepository extends MongoRepository<ResumeEventDocument, String> {

    Optional<ResumeEventDocument> findByResumeIdAndVersionNo(Long resumeId, Integer versionNo);

    Optional<ResumeEventDocument> findFirstByResumeIdAndStatusOrderByVersionNoDesc(
            Long resumeId, ResumeVersionStatus status);

    Optional<ResumeEventDocument>
            findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                    Long resumeId, ResumeVersionStatus status);

    List<ResumeEventDocument> findByUserIdInAndStatusOrderByCreatedAtAsc(
            List<Long> userIds, ResumeVersionStatus status, Pageable pageable);

    boolean existsByUserIdAndStatusIn(Long userId, List<ResumeVersionStatus> statuses);

    boolean existsByResumeIdAndStatusIn(Long resumeId, List<ResumeVersionStatus> statuses);

    Optional<ResumeEventDocument> findTopByResumeIdOrderByVersionNoDesc(Long resumeId);

    Optional<ResumeEventDocument> findByAiTaskId(String aiTaskId);

    List<ResumeEventDocument> findByStatusIn(List<ResumeVersionStatus> statuses);

    void deleteByResumeId(Long resumeId);
}
