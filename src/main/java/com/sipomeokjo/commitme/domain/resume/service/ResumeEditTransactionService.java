package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeEditTransactionService {
    private final ResumeFinder resumeFinder;
    private final ResumeEventMongoRepository resumeEventMongoRepository;

    @Transactional
    public EditPrepared prepareEdit(Long userId, Long resumeId) {
        Resume resume = resumeFinder.getByIdAndUserIdOrThrow(resumeId, userId);

        boolean hasPending =
                resumeEventMongoRepository.existsByResumeIdAndStatusIn(
                        resume.getId(),
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));
        if (hasPending) {
            log.warn("[RESUME_EDIT] in_progress userId={} resumeId={}", userId, resumeId);
            throw new BusinessException(ErrorCode.RESUME_EDIT_IN_PROGRESS);
        }

        ResumeEventDocument latestSucceeded =
                resumeEventMongoRepository
                        .findFirstByResumeIdAndStatusOrderByVersionNoDesc(
                                resume.getId(), ResumeVersionStatus.SUCCEEDED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        int nextVersionNo =
                resumeEventMongoRepository
                        .findTopByResumeIdOrderByVersionNoDesc(resume.getId())
                        .map(e -> e.getVersionNo() + 1)
                        .orElse(1);

        ResumeEventDocument next =
                ResumeEventDocument.create(
                        resume.getId(),
                        nextVersionNo,
                        userId,
                        ResumeVersionStatus.QUEUED,
                        latestSucceeded.getSnapshot());
        resumeEventMongoRepository.save(next);

        return new EditPrepared(
                resume.getId(), resume.getName(), nextVersionNo, latestSucceeded.getSnapshot());
    }

    public ResumeEventDocument markEditRequested(Long resumeId, Integer versionNo, String jobId) {
        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        event.startProcessing(jobId, Instant.now());
        return resumeEventMongoRepository.save(event);
    }

    public void markEditFailed(Long resumeId, Integer versionNo, String errorMessage) {
        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        event.failNow("AI_EDIT_FAILED", errorMessage);
        resumeEventMongoRepository.save(event);
    }

    public record EditPrepared(
            Long resumeId, String resumeName, Integer versionNo, String baseContent) {}
}
