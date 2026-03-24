package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeEditTransactionService {
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeProjectionService resumeProjectionService;

    @Transactional
    public EditPrepared prepareEdit(Long userId, Long resumeId) {
        ResumeDocument doc = resumeProjectionService.markPendingIfIdleOrThrow(resumeId, userId);

        ResumeEventDocument latestSucceeded =
                resumeEventMongoRepository
                        .findFirstByResumeIdAndStatusOrderByVersionNoDesc(
                                doc.getResumeId(), ResumeVersionStatus.SUCCEEDED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        int nextVersionNo =
                resumeEventMongoRepository
                        .findTopByResumeIdOrderByVersionNoDesc(doc.getResumeId())
                        .map(e -> e.getVersionNo() + 1)
                        .orElse(1);

        ResumeEventDocument next =
                ResumeEventDocument.create(
                        doc.getResumeId(),
                        nextVersionNo,
                        userId,
                        ResumeVersionStatus.QUEUED,
                        latestSucceeded.getSnapshot());
        try {
            resumeEventMongoRepository.save(next);
        } catch (DuplicateKeyException e) {
            log.warn(
                    "[RESUME_EDIT] version_no_conflict resumeId={} versionNo={}",
                    doc.getResumeId(),
                    nextVersionNo);
            throw new BusinessException(ErrorCode.RESUME_EDIT_IN_PROGRESS);
        }

        return new EditPrepared(
                doc.getResumeId(), doc.getName(), nextVersionNo, latestSucceeded.getSnapshot());
    }

    @Transactional("mongoTransactionManager")
    public ResumeEventDocument markEditRequested(Long resumeId, Integer versionNo, String jobId) {
        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        event.startProcessing(jobId, Instant.now());
        return resumeEventMongoRepository.save(event);
    }

    @Transactional
    public void markEditFailed(Long resumeId, Integer versionNo, String errorMessage) {
        resumeEventMongoRepository
                .findByResumeIdAndVersionNo(resumeId, versionNo)
                .ifPresentOrElse(
                        event -> {
                            event.failNow("AI_EDIT_FAILED", errorMessage);
                            resumeEventMongoRepository.save(event);
                        },
                        () ->
                                log.error(
                                        "[RESUME_EDIT] mark_failed_event_not_found resumeId={} versionNo={}",
                                        resumeId,
                                        versionNo));
    }

    public record EditPrepared(
            Long resumeId, String resumeName, Integer versionNo, String baseContent) {}
}
