package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ResumeEventMongoRepository extends MongoRepository<ResumeEventDocument, String> {

    Optional<ResumeEventDocument> findByResumeIdAndVersionNo(Long resumeId, Integer versionNo);

    Optional<ResumeEventDocument> findFirstByResumeIdAndStatusOrderByVersionNoDesc(
            Long resumeId, ResumeVersionStatus status);

    Optional<ResumeEventDocument>
            findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                    Long resumeId, ResumeVersionStatus status);

    List<ResumeEventDocument> findByUserIdInAndStatusOrderByCreatedAtAsc(
            List<Long> userIds, ResumeVersionStatus status, Pageable pageable);

    List<ResumeEventDocument> findByUserIdInAndStatusInOrderByCreatedAtAsc(
            List<Long> userIds, List<ResumeVersionStatus> statuses, Pageable pageable);

    List<ResumeEventDocument> findByResumeIdInAndStatusIn(
            List<Long> resumeIds, List<ResumeVersionStatus> statuses, Pageable pageable);

    boolean existsByUserIdAndStatusIn(Long userId, List<ResumeVersionStatus> statuses);

    List<ResumeEventDocument> findByUserIdAndStatusIn(
            Long userId, List<ResumeVersionStatus> statuses);

    @Query(value = "{ 'user_id': ?0, 'status': ?1 }", fields = "{ 'resume_id': 1, '_id': 0 }")
    List<ResumeEventDocument> findResumeIdsByUserIdAndStatus(
            Long userId, ResumeVersionStatus status);

    boolean existsByResumeIdAndStatusIn(Long resumeId, List<ResumeVersionStatus> statuses);

    Optional<ResumeEventDocument> findTopByResumeIdOrderByVersionNoDesc(Long resumeId);

    List<ResumeEventDocument> findByResumeIdOrderByVersionNoAsc(Long resumeId);

    Optional<ResumeEventDocument> findByAiTaskId(String aiTaskId);

    List<ResumeEventDocument> findByStatusIn(List<ResumeVersionStatus> statuses);

    long countByResumeIdIn(List<Long> resumeIds);

    void deleteByResumeId(Long resumeId);

    void deleteByResumeIdIn(List<Long> resumeIds);
}
