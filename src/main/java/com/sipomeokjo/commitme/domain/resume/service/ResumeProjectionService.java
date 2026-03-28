package com.sipomeokjo.commitme.domain.resume.service;

import com.mongodb.MongoException;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private final MongoTemplate mongoTemplate;

    public void createProjection(ResumeDocument document) {
        resumeMongoRepository.save(document);
    }

    @Transactional("mongoTransactionManager")
    public void createProjectionWithPreAcquiredLock(
            ResumeDocument projection, ResumeEventDocument event) {
        resumeMongoRepository.save(projection);
        resumeEventMongoRepository.save(event);
    }

    @Transactional("mongoTransactionManager")
    public void createProjectionIfNoPendingOrThrow(
            Long userId, ResumeDocument projection, ResumeEventDocument event) {
        if (resumeMongoRepository.existsByUserIdAndHasPendingWorkTrue(userId)) {
            throw new BusinessException(ErrorCode.RESUME_GENERATION_IN_PROGRESS);
        }
        resumeMongoRepository.save(projection);
        resumeEventMongoRepository.save(event);
    }

    public ResumeDocument findDocumentByResumeIdAndUserIdOrThrow(Long resumeId, Long userId) {
        ResumeDocument doc = getByResumeIdOrThrow(resumeId);
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        return doc;
    }

    public void setPendingWorkStarted(Long resumeId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("resume_id").is(resumeId)),
                new Update().set("has_pending_work", true).set("updated_at", Instant.now()),
                ResumeDocument.class);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, random = true))
    public void applyAiSuccess(Long resumeId, int versionNo, boolean isCreate) {
        Criteria criteria =
                Criteria.where("resume_id")
                        .is(resumeId)
                        .andOperator(
                                new Criteria()
                                        .orOperator(
                                                Criteria.where("last_applied_version_no").isNull(),
                                                Criteria.where("last_applied_version_no")
                                                        .lt(versionNo)));
        Update update =
                new Update()
                        .set("latest_succeeded_version_no", versionNo)
                        .set("latest_preview_version_no", versionNo)
                        .set("has_unseen_preview", true)
                        .set("has_pending_work", false)
                        .set("last_applied_version_no", versionNo)
                        .set("updated_at", Instant.now());
        if (isCreate) {
            update.set("current_version_no", versionNo);
        }
        mongoTemplate.findAndModify(Query.query(criteria), update, ResumeDocument.class);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, random = true))
    public void applyAiFailure(Long resumeId, int versionNo) {
        Criteria criteria =
                Criteria.where("resume_id")
                        .is(resumeId)
                        .andOperator(
                                new Criteria()
                                        .orOperator(
                                                Criteria.where("last_applied_version_no").isNull(),
                                                Criteria.where("last_applied_version_no")
                                                        .lt(versionNo)));
        Update update =
                new Update()
                        .set("has_pending_work", false)
                        .set("last_applied_version_no", versionNo)
                        .set("updated_at", Instant.now());
        mongoTemplate.findAndModify(Query.query(criteria), update, ResumeDocument.class);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, random = true))
    public void applyVersionCommitted(Long resumeId, int versionNo) {
        Criteria criteria = Criteria.where("resume_id").is(resumeId);
        Update update =
                new Update()
                        .set("current_version_no", versionNo)
                        .max("last_applied_version_no", versionNo)
                        .set("updated_at", Instant.now());
        mongoTemplate.findAndModify(Query.query(criteria), update, ResumeDocument.class);
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverApplyAiSuccess(
            MongoException e, Long resumeId, int versionNo, boolean isCreate) {
        log.error(
                "[PROJECTION] applyAiSuccess_failed_after_retries resumeId={} versionNo={} — attempting hasPendingWork clear",
                resumeId,
                versionNo,
                e);
        tryForceClearPendingWork(resumeId);
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverApplyAiFailure(MongoException e, Long resumeId, int versionNo) {
        log.error(
                "[PROJECTION] applyAiFailure_failed_after_retries resumeId={} versionNo={} — attempting hasPendingWork clear",
                resumeId,
                versionNo,
                e);
        tryForceClearPendingWork(resumeId);
    }

    private void tryForceClearPendingWork(Long resumeId) {
        try {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("resume_id").is(resumeId)),
                    new Update().set("has_pending_work", false).set("updated_at", Instant.now()),
                    ResumeDocument.class);
            log.warn(
                    "[PROJECTION] hasPendingWork_cleared resumeId={} — rebuildForResume recommended",
                    resumeId);
        } catch (Exception ex) {
            log.error(
                    "[PROJECTION] hasPendingWork_clear_failed resumeId={} — manual rebuildForResume required",
                    resumeId,
                    ex);
        }
    }

    @Recover
    @SuppressWarnings("unused")
    public void recoverProjectionWrite(MongoException e) {
        log.error(
                "[PROJECTION] write_failed_after_retries — manual rebuildForResume required. error={}",
                e.getMessage(),
                e);
    }

    public void applyPreviewShown(Long resumeId) {
        resumeMongoQueryRepository.clearUnseenPreviewIfPresent(resumeId);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, random = true))
    public void applyProfileSnapshotUpdate(Long resumeId, String json) {
        Criteria criteria = Criteria.where("resume_id").is(resumeId);
        Update update = new Update().set("profile_snapshot", json).set("updated_at", Instant.now());
        mongoTemplate.findAndModify(Query.query(criteria), update, ResumeDocument.class);
    }

    @Retryable(
            retryFor = MongoException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0, random = true))
    public void applyNameChange(Long resumeId, String name) {
        Criteria criteria = Criteria.where("resume_id").is(resumeId);
        Update update = new Update().set("name", name).set("updated_at", Instant.now());
        mongoTemplate.findAndModify(Query.query(criteria), update, ResumeDocument.class);
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
}
