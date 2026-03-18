package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResumeEventQueryRepository {
    private final ResumeEventMongoRepository resumeEventMongoRepository;

    public Optional<ResumeEventDocument> findLatestSucceededByResumeId(Long resumeId) {
        return resumeEventMongoRepository.findFirstByResumeIdAndStatusOrderByVersionNoDesc(
                resumeId, ResumeVersionStatus.SUCCEEDED);
    }

    public Optional<ResumeEventDocument> findLatestUnseenPreviewByResumeId(Long resumeId) {
        return resumeEventMongoRepository
                .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                        resumeId, ResumeVersionStatus.SUCCEEDED);
    }

    public List<ResumeEventDocument> findProcessingByUserIds(List<Long> userIds, int limit) {
        if (userIds == null || userIds.isEmpty() || limit <= 0) {
            return List.of();
        }
        return resumeEventMongoRepository.findByUserIdInAndStatusOrderByCreatedAtAsc(
                userIds, ResumeVersionStatus.PROCESSING, PageRequest.of(0, limit));
    }

    public boolean existsByUserIdAndStatusIn(Long userId, List<ResumeVersionStatus> statuses) {
        return resumeEventMongoRepository.existsByUserIdAndStatusIn(userId, statuses);
    }
}
