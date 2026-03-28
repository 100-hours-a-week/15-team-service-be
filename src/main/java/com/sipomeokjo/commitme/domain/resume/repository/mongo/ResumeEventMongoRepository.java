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

    List<ResumeEventDocument> findByUserIdInAndStatusInOrderByCreatedAtAsc(
            List<Long> userIds, List<ResumeVersionStatus> statuses, Pageable pageable);

    List<ResumeEventDocument> findByResumeIdInAndStatusIn(
            List<Long> resumeIds, List<ResumeVersionStatus> statuses, Pageable pageable);

    boolean existsByResumeIdAndStatusIn(Long resumeId, List<ResumeVersionStatus> statuses);

    boolean existsByResumeIdAndIsPendingTrue(Long resumeId);

    Optional<ResumeEventDocument> findTopByResumeIdOrderByVersionNoDesc(Long resumeId);

    List<ResumeEventDocument> findByResumeIdOrderByVersionNoAsc(Long resumeId);

    List<ResumeEventDocument> findByStatusIn(List<ResumeVersionStatus> statuses);

    long countByResumeIdIn(List<Long> resumeIds);

    void deleteByResumeId(Long resumeId);

    void deleteByResumeIdIn(List<Long> resumeIds);

    List<ResumeEventDocument> findByResumeIdAndStatusAndCommittedAtIsNotNullOrderByVersionNoDesc(
            Long resumeId, ResumeVersionStatus status, Pageable pageable);

    List<ResumeEventDocument>
            findByResumeIdAndStatusAndCommittedAtIsNotNullAndVersionNoLessThanOrderByVersionNoDesc(
                    Long resumeId,
                    ResumeVersionStatus status,
                    Integer versionNo,
                    Pageable pageable);
}
