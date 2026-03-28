package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.validation.KeywordValidator;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.outbox.dto.OutboxEventTypes;
import com.sipomeokjo.commitme.domain.outbox.repository.OutboxEventRepository;
import com.sipomeokjo.commitme.domain.outbox.service.OutboxEventService;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeEditResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeRenameRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSortBy;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionSummaryDto;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeGenerateOutboxPayload;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeMapper;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.global.mongo.MongoSequenceService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeService {
    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeMongoQueryRepository resumeMongoQueryRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeFinder resumeFinder;
    private final MongoSequenceService mongoSequenceService;
    private final PositionFinder positionFinder;
    private final CompanyRepository companyRepository;
    private final CursorParser cursorParser;
    private final ResumeMapper resumeMapper;
    private final ResumeProfileService resumeProfileService;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventService outboxEventService;
    private final ResumeAiRequestService resumeAiRequestService;
    private final ResumeEditTransactionService resumeEditTransactionService;
    private final ResumeProjectionService resumeProjectionService;
    private final ResumeLockService resumeLockService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CursorResponse<ResumeSummaryDto> list(
            Long userId, CursorRequest request, String keyword, String sortedBy) {
        ResumeSortBy sortBy = ResumeSortBy.from(sortedBy);
        CursorParser.Cursor cursor =
                cursorParser.parseCompositeCursor(request == null ? null : request.next());
        int size = CursorRequest.resolveLimit(request, 10);
        String normalizedKeyword = KeywordValidator.normalize(keyword, 30);

        List<ResumeDocument> docs =
                (sortBy == ResumeSortBy.UPDATED_ASC)
                        ? resumeMongoQueryRepository.findByUserIdWithCursorAsc(
                                userId,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                size + 1)
                        : resumeMongoQueryRepository.findByUserIdWithCursorDesc(
                                userId,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                size + 1);

        boolean hasMore = docs.size() > size;
        List<ResumeDocument> page = hasMore ? docs.subList(0, size) : docs;

        List<ResumeSummaryDto> items =
                page.stream().map(resumeMapper::toSummaryDtoFromDocument).toList();

        String next = hasMore && !page.isEmpty() ? encodeCursor(page.getLast()) : null;
        return new CursorResponse<>(items, null, next);
    }

    private String encodeCursor(ResumeDocument doc) {
        return doc.getUpdatedAt() + "|" + doc.getResumeId();
    }

    public Long create(Long userId, ResumeCreateRequest req) {

        if (req.getRepoUrls() == null || req.getRepoUrls().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (req.getPositionId() == null)
            throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
        Position position = positionFinder.getByIdOrThrow(req.getPositionId());

        Company company = null;
        if (req.getCompanyId() != null) {
            company =
                    companyRepository
                            .findById(req.getCompanyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(clock);
            String base =
                    String.format(
                            "%04d%02d%02d_%02d:%02d_%s",
                            now.getYear(),
                            now.getMonthValue(),
                            now.getDayOfMonth(),
                            now.getHour(),
                            now.getMinute(),
                            position.getName());
            name = (company != null) ? base + "_" + company.getName() : base;
        }
        if (name.length() > 30) throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);

        String createLockToken = UUID.randomUUID().toString();
        ResumeLockService.LockAcquireResult lockAcquireResult =
                resumeLockService.tryAcquireCreateLock(userId, createLockToken);
        if (lockAcquireResult == ResumeLockService.LockAcquireResult.BUSY) {
            throw new BusinessException(ErrorCode.RESUME_GENERATION_IN_PROGRESS);
        }

        Long resumeId = null;
        boolean projectionCreated = false;
        boolean redisLockBoundToResumeId = false;
        try {
            resumeId = mongoSequenceService.nextResumeId();
            if (lockAcquireResult == ResumeLockService.LockAcquireResult.ACQUIRED) {
                redisLockBoundToResumeId =
                        resumeLockService.bindCreateLockOwner(userId, createLockToken, resumeId);
                if (!redisLockBoundToResumeId) {
                    throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
                }
            }

            ResumeEventDocument event =
                    ResumeEventDocument.create(
                            resumeId, 1, userId, ResumeVersionStatus.QUEUED, "{}");

            ResumeDocument projection =
                    ResumeDocument.create(
                            resumeId,
                            userId,
                            position.getId(),
                            position.getName(),
                            company != null ? company.getId() : null,
                            company != null ? company.getName() : null,
                            name,
                            null);

            if (lockAcquireResult == ResumeLockService.LockAcquireResult.FALLBACK) {
                resumeProjectionService.createProjectionIfNoPendingOrThrow(
                        userId, projection, event);
            } else {
                resumeProjectionService.createProjectionWithPreAcquiredLock(projection, event);
            }
            projectionCreated = true;

            outboxEventService.enqueue(
                    OutboxEventTypes.AI_JOB_REQUESTED,
                    "RESUME_EVENT",
                    String.valueOf(resumeId),
                    new ResumeGenerateOutboxPayload(
                            resumeId, 1, userId, position.getName(), req.getRepoUrls()));
        } catch (Exception e) {
            if (projectionCreated) {
                markCreateFailedAndLog(userId, resumeId, e.getMessage());
            }
            if (lockAcquireResult == ResumeLockService.LockAcquireResult.ACQUIRED) {
                if (redisLockBoundToResumeId && resumeId != null) {
                    resumeLockService.releaseCreateLock(userId, resumeId);
                } else {
                    resumeLockService.releaseCreateLockByToken(userId, createLockToken);
                }
            }
            throw e;
        }

        return resumeId;
    }

    @Transactional
    public ResumeDetailDto get(Long userId, Long resumeId) {

        ResumeDocument doc = resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);
        ResumeProfileResponse profileResponse =
                resumeProfileService.getProfile(
                        userId, doc.getResumeId(), doc.getProfileSnapshot());
        boolean isEditing =
                doc.isHasPendingWork()
                        || resumeEventMongoRepository.existsByResumeIdAndIsPendingTrue(
                                doc.getResumeId());

        ResumeEventDocument previewEvent =
                resumeEventMongoRepository
                        .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                                doc.getResumeId(), ResumeVersionStatus.SUCCEEDED)
                        .filter(v -> !v.getVersionNo().equals(doc.getCurrentVersionNo()))
                        .orElse(null);

        if (previewEvent != null) {
            previewEvent.markPreviewShown(Instant.now());
            resumeEventMongoRepository.save(previewEvent);
            resumeProjectionService.applyPreviewShown(doc.getResumeId());
            return resumeMapper.toDetailDtoFromDocument(
                    doc, previewEvent, isEditing, profileResponse);
        }

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(doc.getResumeId(), doc.getCurrentVersionNo())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        return resumeMapper.toDetailDtoFromDocument(doc, event, isEditing, profileResponse);
    }

    @Transactional(readOnly = true)
    public CursorResponse<ResumeVersionSummaryDto> getVersionList(
            Long userId, Long resumeId, CursorRequest request) {

        resumeProjectionService.findDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);

        int size = CursorRequest.resolveLimit(request, 50);

        Integer cursorVersionNo = null;
        if (request != null && request.next() != null) {
            try {
                cursorVersionNo = Integer.parseInt(request.next());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
        }

        boolean isFirstPage = (cursorVersionNo == null);

        List<ResumeEventDocument> events;
        if (isFirstPage) {
            events =
                    resumeEventMongoRepository
                            .findByResumeIdAndStatusAndCommittedAtIsNotNullOrderByVersionNoDesc(
                                    resumeId,
                                    ResumeVersionStatus.SUCCEEDED,
                                    PageRequest.of(0, size + 1));
        } else {
            events =
                    resumeEventMongoRepository
                            .findByResumeIdAndStatusAndCommittedAtIsNotNullAndVersionNoLessThanOrderByVersionNoDesc(
                                    resumeId,
                                    ResumeVersionStatus.SUCCEEDED,
                                    cursorVersionNo,
                                    PageRequest.of(0, size + 1));
        }

        boolean hasMore = events.size() > size;
        List<ResumeEventDocument> page = hasMore ? events.subList(0, size) : events;

        List<ResumeVersionSummaryDto> items =
                new ArrayList<>(
                        page.stream()
                                .map(
                                        e ->
                                                new ResumeVersionSummaryDto(
                                                        e.getVersionNo(), e.getCommittedAt()))
                                .toList());

        if (isFirstPage) {
            resumeEventMongoRepository
                    .findFirstByResumeIdAndStatusOrderByVersionNoDesc(
                            resumeId, ResumeVersionStatus.SUCCEEDED)
                    .ifPresent(
                            e -> {
                                if (e.getCommittedAt() == null) {
                                    items.addFirst(
                                            new ResumeVersionSummaryDto(e.getVersionNo(), null));
                                }
                            });
        }

        String next =
                hasMore && !page.isEmpty() ? String.valueOf(page.getLast().getVersionNo()) : null;
        return new CursorResponse<>(items, null, next);
    }

    @Transactional(readOnly = true)
    public boolean existsByResumeIdAndUserId(Long resumeId, Long userId) {
        return resumeMongoRepository.existsByResumeIdAndUserId(resumeId, userId);
    }

    @Transactional
    public ResumeVersionDto getVersion(Long userId, Long resumeId, int versionNo) {

        ResumeDocument doc = resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(doc.getResumeId(), versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.isProcessingTimedOut(ResumeVersionTimeoutService.AI_PROCESSING_TIMEOUT_MINUTES)) {
            event.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
            resumeEventMongoRepository.save(event);
        }

        return new ResumeVersionDto(
                doc.getResumeId(),
                event.getVersionNo(),
                event.getStatus(),
                event.getSnapshot(),
                event.getAiTaskId(),
                event.getErrorLog(),
                event.getStartedAt(),
                event.getFinishedAt(),
                event.getCommittedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    public void rename(Long userId, Long resumeId, ResumeRenameRequest req) {

        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 30)
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);

        resumeProjectionService.findDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);
        resumeProjectionService.applyNameChange(resumeId, name);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResumeEditResponse edit(Long userId, Long resumeId, ResumeEditRequest req) {
        String message = (req == null || req.message() == null) ? "" : req.message().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String editLockToken = UUID.randomUUID().toString();
        ResumeLockService.LockAcquireResult lockAcquireResult =
                resumeLockService.tryAcquireEditLock(resumeId, editLockToken);
        boolean redisLockHeld = lockAcquireResult == ResumeLockService.LockAcquireResult.ACQUIRED;
        if (lockAcquireResult == ResumeLockService.LockAcquireResult.BUSY) {
            throw new BusinessException(ErrorCode.RESUME_EDIT_IN_PROGRESS);
        }

        ResumeEditTransactionService.EditPrepared prepared;
        Integer previewNextVersionNo = null;
        boolean redisLockBoundToVersion = false;
        try {
            if (lockAcquireResult == ResumeLockService.LockAcquireResult.FALLBACK) {
                prepared = resumeEditTransactionService.prepareEdit(userId, resumeId);
            } else {
                previewNextVersionNo = resumeEditTransactionService.peekNextVersionNo(resumeId);
                redisLockBoundToVersion =
                        resumeLockService.bindEditLockOwner(
                                resumeId, editLockToken, previewNextVersionNo);
                if (!redisLockBoundToVersion) {
                    throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
                }
                prepared =
                        resumeEditTransactionService.prepareEditWithPreAcquiredLock(
                                userId, resumeId, previewNextVersionNo);
            }
        } catch (Exception e) {
            if (redisLockHeld) {
                if (redisLockBoundToVersion && previewNextVersionNo != null) {
                    resumeLockService.releaseEditLock(resumeId, previewNextVersionNo);
                } else {
                    resumeLockService.releaseEditLockByToken(resumeId, editLockToken);
                }
            }
            throw e;
        }

        try {
            String jobId =
                    resumeAiRequestService.requestEdit(
                            prepared.resumeId(), prepared.baseContent(), message);
            ResumeEventDocument updated =
                    resumeEditTransactionService.markEditRequested(
                            prepared.resumeId(), prepared.versionNo(), jobId);
            log.debug(
                    "[RESUME_EDIT] ai_requested userId={} resumeId={} versionNo={} taskId={}",
                    userId,
                    resumeId,
                    prepared.versionNo(),
                    jobId);
            return new ResumeEditResponse(
                    prepared.resumeId(),
                    prepared.versionNo(),
                    prepared.resumeName(),
                    updated.getAiTaskId(),
                    updated.getUpdatedAt());
        } catch (BusinessException e) {
            markEditFailedAndLog(userId, resumeId, prepared, e.getMessage());
            if (redisLockHeld) {
                resumeLockService.releaseEditLock(resumeId, prepared.versionNo());
            }
            throw e;
        } catch (Exception e) {
            markEditFailedAndLog(userId, resumeId, prepared, e.getMessage());
            if (redisLockHeld) {
                resumeLockService.releaseEditLock(resumeId, prepared.versionNo());
            }
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void markEditFailedAndLog(
            Long userId,
            Long resumeId,
            ResumeEditTransactionService.EditPrepared prepared,
            String errorMessage) {
        resumeEditTransactionService.markEditFailed(
                prepared.resumeId(), prepared.versionNo(), errorMessage);
        log.warn(
                "[RESUME_EDIT] ai_failed userId={} resumeId={} versionNo={} error={}",
                userId,
                resumeId,
                prepared.versionNo(),
                errorMessage);
    }

    private void markCreateFailedAndLog(Long userId, Long resumeId, String errorMessage) {
        resumeEventMongoRepository
                .findByResumeIdAndVersionNo(resumeId, 1)
                .ifPresentOrElse(
                        event -> {
                            event.failNow("AI_GENERATE_FAILED", errorMessage);
                            resumeEventMongoRepository.save(event);
                            resumeProjectionService.applyAiFailure(resumeId, event.getVersionNo());
                        },
                        () ->
                                log.error(
                                        "[RESUME_CREATE] mark_failed_event_not_found userId={} resumeId={}",
                                        userId,
                                        resumeId));
        log.warn(
                "[RESUME_CREATE] ai_request_enqueue_failed userId={} resumeId={} error={}",
                userId,
                resumeId,
                errorMessage);
    }

    @Transactional
    public void saveVersion(Long userId, Long resumeId, int versionNo) {

        resumeProjectionService.findDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        event.markCommitted(Instant.now());
        resumeEventMongoRepository.save(event);
        resumeProjectionService.applyVersionCommitted(resumeId, versionNo);
    }

    @Transactional
    public void delete(Long userId, Long resumeId) {
        resumeProjectionService.findDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);

        outboxEventRepository.deleteByAggregateId(String.valueOf(resumeId));

        resumeProjectionService.deleteProjectionAndEvents(resumeId);
    }
}
