package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.validation.KeywordValidator;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.credit.config.AiCreditProperties;
import com.sipomeokjo.commitme.domain.credit.service.AiCreditService;
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
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventAggregationRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventAggregationResult;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.global.mongo.MongoSequenceService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeService {
    private static final String RESUME_EVENT_AGGREGATE_TYPE = "RESUME_EVENT";

    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeMongoQueryRepository resumeMongoQueryRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeEventAggregationRepository resumeEventAggregationRepository;
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
    private final AiCreditService aiCreditService;
    private final AiCreditProperties aiCreditProperties;
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
        boolean isUserProvidedName = !name.isEmpty();
        if (!isUserProvidedName) {
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
        if (isUserProvidedName && name.length() > 30)
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);
        if (name.length() > 30) name = name.substring(0, 30);

        Long resumeId = mongoSequenceService.nextResumeId();

        ResumeEventDocument event =
                ResumeEventDocument.create(resumeId, 1, userId, ResumeVersionStatus.QUEUED, "{}");

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

        aiCreditService.deduct(userId, aiCreditProperties.getResumeGenerateCost());
        resumeProjectionService.createProjectionIfNoPendingOrThrow(userId, projection, event);

        try {
            outboxEventService.enqueue(
                    OutboxEventTypes.AI_JOB_REQUESTED,
                    RESUME_EVENT_AGGREGATE_TYPE,
                    String.valueOf(resumeId),
                    new ResumeGenerateOutboxPayload(
                            resumeId, 1, userId, position.getName(), req.getRepoUrls()));
        } catch (BusinessException e) {
            compensateCreateFailure(resumeId, e.getMessage());
            throw e;
        } catch (Exception e) {
            compensateCreateFailure(resumeId, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }

        return resumeId;
    }

    @Transactional(readOnly = true)
    public ResumeDetailDto get(Long userId, Long resumeId) {

        ResumeDocument doc = resumeFinder.getDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);
        ResumeProfileResponse profileResponse =
                resumeProfileService.getProfile(
                        userId, doc.getResumeId(), doc.getProfileSnapshot());

        boolean isEditing =
                resumeEventMongoRepository.existsByResumeIdAndStatusIn(
                        doc.getResumeId(),
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));

        ResumeEventDocument previewEvent =
                resumeEventMongoRepository
                        .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                                doc.getResumeId(), ResumeVersionStatus.SUCCEEDED)
                        .filter(v -> !v.getVersionNo().equals(doc.getCurrentVersionNo()))
                        .orElse(null);

        if (previewEvent != null) {
            Instant previewShownAt = Instant.now();
            try {
                resumeProjectionService.markPreviewShownAndClearUnseen(
                        doc.getResumeId(), previewEvent.getVersionNo(), previewShownAt);
            } catch (Exception e) {
                log.warn(
                        "[RESUME_GET] preview_mark_failed resumeId={} versionNo={} — will retry on next GET",
                        doc.getResumeId(),
                        previewEvent.getVersionNo(),
                        e);
            }
            previewEvent.markPreviewShown(previewShownAt);
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

        resumeProjectionService.validateOwnershipOrThrow(resumeId, userId);

        int size = CursorRequest.resolveLimit(request, 50);
        Integer cursorVersionNo = parseCursorVersionNo(request);
        boolean isFirstPage = (cursorVersionNo == null);

        ResumeEventAggregationResult.VersionListResult result =
                resumeEventAggregationRepository.findVersionListPage(
                        resumeId, cursorVersionNo, size);

        boolean includePreview =
                isFirstPage
                        && result.latestSucceeded() != null
                        && result.latestSucceeded().getCommittedAt() == null;
        int committedLimit = includePreview ? Math.max(size - 1, 0) : size;

        List<ResumeEventDocument> events = result.committedPage();
        boolean hasMore = events.size() > committedLimit;
        List<ResumeEventDocument> page = events.subList(0, Math.min(events.size(), committedLimit));

        List<ResumeVersionSummaryDto> items =
                new ArrayList<>(
                        page.stream()
                                .map(
                                        e ->
                                                new ResumeVersionSummaryDto(
                                                        e.getVersionNo(), e.getCommittedAt()))
                                .toList());

        if (includePreview) {
            items.addFirst(
                    new ResumeVersionSummaryDto(result.latestSucceeded().getVersionNo(), null));
        }

        String next = hasMore ? resolveNextVersionCursor(page, events) : null;
        return new CursorResponse<>(items, null, next);
    }

    private String resolveNextVersionCursor(
            List<ResumeEventDocument> page, List<ResumeEventDocument> events) {
        ResumeEventDocument nextCursorSource =
                !page.isEmpty() ? page.getLast() : (events.isEmpty() ? null : events.getFirst());
        return nextCursorSource == null ? null : String.valueOf(nextCursorSource.getVersionNo());
    }

    private Integer parseCursorVersionNo(CursorRequest request) {
        if (request == null || request.next() == null) return null;
        try {
            return Integer.parseInt(request.next());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
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

        resumeProjectionService.validateOwnershipOrThrow(resumeId, userId);
        resumeProjectionService.applyNameChange(resumeId, name);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResumeEditResponse edit(Long userId, Long resumeId, ResumeEditRequest req) {
        String message = (req == null || req.message() == null) ? "" : req.message().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        ResumeEditTransactionService.EditPrepared prepared =
                resumeEditTransactionService.prepareEdit(userId, resumeId);

        String jobId;
        try {
            aiCreditService.deduct(userId, aiCreditProperties.getResumeEditCost());
            jobId =
                    resumeAiRequestService.requestEdit(
                            prepared.resumeId(), prepared.baseContent(), message);
        } catch (BusinessException e) {
            markEditFailedAndLog(userId, resumeId, prepared, e.getMessage());
            throw e;
        } catch (Exception e) {
            markEditFailedAndLog(userId, resumeId, prepared, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return markEditRequestedAndBuildResponse(userId, resumeId, prepared, jobId);
    }

    private ResumeEditResponse markEditRequestedAndBuildResponse(
            Long userId,
            Long resumeId,
            ResumeEditTransactionService.EditPrepared prepared,
            String jobId) {
        try {
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
            logEditRequestPersistenceFailure(userId, resumeId, prepared.versionNo(), jobId, e);
            throw e;
        } catch (Exception e) {
            logEditRequestPersistenceFailure(userId, resumeId, prepared.versionNo(), jobId, e);
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

    private void logEditRequestPersistenceFailure(
            Long userId, Long resumeId, Integer versionNo, String jobId, Exception exception) {
        log.error(
                "[RESUME_EDIT] ai_requested_but_mark_processing_failed userId={} resumeId={} versionNo={} taskId={} error={}",
                userId,
                resumeId,
                versionNo,
                jobId,
                exception.getMessage(),
                exception);
    }

    @Transactional
    public void saveVersion(Long userId, Long resumeId, int versionNo) {

        resumeProjectionService.validateOwnershipOrThrow(resumeId, userId);

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }
        if (event.getCommittedAt() != null) {
            return;
        }

        resumeProjectionService.commitVersionAndApplyProjection(resumeId, versionNo, Instant.now());
    }

    @Transactional
    public void delete(Long userId, Long resumeId) {
        resumeProjectionService.validateOwnershipOrThrow(resumeId, userId);

        outboxEventRepository.deleteByAggregateId(String.valueOf(resumeId));

        resumeProjectionService.deleteProjectionAndEvents(resumeId);
    }

    private void compensateCreateFailure(Long resumeId, String errorMessage) {
        try {
            resumeProjectionService.applyCreateCompensation(resumeId, 1, errorMessage);
        } catch (Exception ex) {
            log.error(
                    "[RESUME_CREATE] compensation_failed resumeId={} error={}",
                    resumeId,
                    ex.getMessage(),
                    ex);
        }
    }
}
