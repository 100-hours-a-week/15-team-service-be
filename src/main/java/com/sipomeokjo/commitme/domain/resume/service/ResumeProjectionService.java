package com.sipomeokjo.commitme.domain.resume.service;

import com.mongodb.MongoException;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProjectionService {

    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeMongoQueryRepository resumeMongoQueryRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;

    public void createProjection(ResumeDocument document) {
        resumeMongoRepository.save(document);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0))
    public void applyAiSuccess(Long resumeId, int versionNo, boolean isCreate) {
        ResumeDocument doc = getByResumeIdOrThrow(resumeId);
        doc.applyAiSuccess(versionNo, isCreate);
        resumeMongoRepository.save(doc);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0))
    public void applyAiFailure(Long resumeId, int versionNo) {
        ResumeDocument doc = getByResumeIdOrThrow(resumeId);
        doc.applyAiFailure(versionNo);
        resumeMongoRepository.save(doc);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0))
    public void applyVersionCommitted(Long resumeId, int versionNo) {
        ResumeDocument doc = getByResumeIdOrThrow(resumeId);
        doc.applyVersionCommitted(versionNo);
        resumeMongoRepository.save(doc);
    }

    /** Single recover for all @Retryable write methods (return type void + MongoException). */
    @Recover
    public void recoverProjectionWrite(MongoException e) {
        log.warn("[PROJECTION] write_failed_after_retries error={}", e.getMessage());
    }

    public void applyPreviewShown(Long resumeId) {
        resumeMongoQueryRepository.clearUnseenPreviewIfPresent(resumeId);
    }

    public void applyProfileSnapshotUpdate(Long resumeId, String json) {
        resumeMongoRepository
                .findByResumeId(resumeId)
                .ifPresent(
                        doc -> {
                            doc.applyProfileSnapshot(json);
                            resumeMongoRepository.save(doc);
                        });
    }

    public void applyNameChange(Long resumeId, String name) {
        resumeMongoRepository
                .findByResumeId(resumeId)
                .ifPresent(
                        doc -> {
                            doc.applyNameChange(name);
                            resumeMongoRepository.save(doc);
                        });
    }

    public void markPendingWorkStarted(Long resumeId) {
        resumeMongoRepository
                .findByResumeId(resumeId)
                .ifPresent(
                        doc -> {
                            doc.markPendingWorkStarted();
                            resumeMongoRepository.save(doc);
                        });
    }

    @Transactional("mongoTransactionManager")
    public void deleteProjectionAndEvents(Long resumeId) {
        resumeMongoRepository.deleteByResumeId(resumeId);
        resumeEventMongoRepository.deleteByResumeId(resumeId);
    }

    public ResumeDocument getByResumeIdOrThrow(Long resumeId) {
        return resumeMongoRepository
                .findByResumeId(resumeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    public ResumeDocument getByResumeIdAndUserIdOrThrow(Long resumeId, Long userId) {
        ResumeDocument doc = getByResumeIdOrThrow(resumeId);
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        return doc;
    }
}
