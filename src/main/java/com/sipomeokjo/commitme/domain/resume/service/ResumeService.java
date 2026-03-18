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
import com.sipomeokjo.commitme.domain.outbox.service.OutboxEventService;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeGenerateOutboxPayload;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeMapper;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.service.UserFinder;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final ResumeRepository resumeRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final UserFinder userFinder;
    private final ResumeFinder resumeFinder;
    private final PositionFinder positionFinder;
    private final CompanyRepository companyRepository;
    private final CursorParser cursorParser;
    private final ResumeMapper resumeMapper;
    private final ResumeProfileService resumeProfileService;

    private final OutboxEventService outboxEventService;
    private final ResumeAiRequestService resumeAiRequestService;
    private final ResumeEditTransactionService resumeEditTransactionService;

    @Transactional(readOnly = true)
    public CursorResponse<ResumeSummaryDto> list(
            Long userId, CursorRequest request, String keyword, String sortedBy) {
        ResumeSortBy sortBy = ResumeSortBy.from(sortedBy);
        CursorParser.Cursor cursor =
                cursorParser.parseCompositeCursor(request == null ? null : request.next());
        int size = CursorRequest.resolveLimit(request, 10);
        String normalizedKeyword = KeywordValidator.normalize(keyword, 30);

        Set<Long> succeededIds =
                resumeEventMongoRepository
                        .findResumeIdsByUserIdAndStatus(userId, ResumeVersionStatus.SUCCEEDED)
                        .stream()
                        .map(ResumeEventDocument::getResumeId)
                        .collect(Collectors.toSet());

        if (succeededIds.isEmpty()) {
            return new CursorResponse<>(List.of(), null, null);
        }

        List<Resume> resumes =
                (sortBy == ResumeSortBy.UPDATED_ASC)
                        ? resumeRepository.findSucceededByUserIdWithCursorAsc(
                                succeededIds,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                PageRequest.of(0, size + 1))
                        : resumeRepository.findSucceededByUserIdWithCursorDesc(
                                succeededIds,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                PageRequest.of(0, size + 1));

        boolean hasMore = resumes.size() > size;
        List<Resume> pageResumes = hasMore ? resumes.subList(0, size) : resumes;

        Set<Long> editingResumeIds =
                resumeEventMongoRepository
                        .findByUserIdAndStatusIn(
                                userId,
                                List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING))
                        .stream()
                        .map(ResumeEventDocument::getResumeId)
                        .collect(Collectors.toSet());

        List<ResumeSummaryDto> items =
                pageResumes.stream()
                        .map(
                                r ->
                                        resumeMapper.toSummaryDto(
                                                r, editingResumeIds.contains(r.getId())))
                        .toList();

        String next =
                hasMore && !pageResumes.isEmpty() ? encodeCursor(pageResumes.getLast()) : null;
        return new CursorResponse<>(items, null, next);
    }

    private String encodeCursor(Resume resume) {
        return resume.getUpdatedAt() + "|" + resume.getId();
    }

    public Long create(Long userId, ResumeCreateRequest req) {

        User user = userFinder.getByIdWithLockOrThrow(userId);

        boolean hasPending =
                resumeEventMongoRepository.existsByUserIdAndStatusIn(
                        userId,
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));
        if (hasPending) {
            throw new BusinessException(ErrorCode.RESUME_GENERATION_IN_PROGRESS);
        }

        if (req.getRepoUrls() == null || req.getRepoUrls().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (req.getPositionId() == null)
            throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
        Position position = positionFinder.getByIdOrThrow(req.getPositionId());

        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            name =
                    String.format(
                            "%04d%02d%02d%02d:%02d%s",
                            now.getYear(),
                            now.getMonthValue(),
                            now.getDayOfMonth(),
                            now.getHour(),
                            now.getMinute(),
                            position.getName());
        }
        if (name.length() > 30) throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);

        Company company = null;
        if (req.getCompanyId() != null) {
            company =
                    companyRepository
                            .findById(req.getCompanyId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        Resume resume = Resume.create(user, position, company, name);
        Resume saved = resumeRepository.save(resume);

        ResumeEventDocument event =
                ResumeEventDocument.create(
                        saved.getId(), 1, userId, ResumeVersionStatus.QUEUED, "{}");
        resumeEventMongoRepository.save(event);

        outboxEventService.enqueue(
                OutboxEventTypes.AI_JOB_REQUESTED,
                "RESUME_EVENT",
                String.valueOf(saved.getId()),
                new ResumeGenerateOutboxPayload(
                        saved.getId(), 1, userId, position.getName(), req.getRepoUrls()));

        return saved.getId();
    }

    public ResumeDetailDto get(Long userId, Long resumeId) {

        Resume resume = resumeFinder.getByIdAndUserIdOrThrow(resumeId, userId);
        ResumeProfileResponse profileResponse = resumeProfileService.getProfile(userId, resume);
        boolean isEditing =
                resumeEventMongoRepository.existsByResumeIdAndStatusIn(
                        resume.getId(),
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));

        ResumeEventDocument previewEvent =
                resumeEventMongoRepository
                        .findFirstByResumeIdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                                resume.getId(), ResumeVersionStatus.SUCCEEDED)
                        .filter(v -> !v.getVersionNo().equals(resume.getCurrentVersionNo()))
                        .orElse(null);

        if (previewEvent != null) {
            previewEvent.markPreviewShown(Instant.now());
            resumeEventMongoRepository.save(previewEvent);
            return resumeMapper.toDetailDto(resume, previewEvent, isEditing, profileResponse);
        }

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resume.getId(), resume.getCurrentVersionNo())
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        return resumeMapper.toDetailDto(resume, event, isEditing, profileResponse);
    }

    @Transactional(readOnly = true)
    public boolean existsByResumeIdAndUserId(Long resumeId, Long userId) {
        return resumeRepository.existsByIdAndUser_Id(resumeId, userId);
    }

    public ResumeVersionDto getVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resume.getId(), versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.isProcessingTimedOut(ResumeVersionTimeoutService.AI_PROCESSING_TIMEOUT_MINUTES)) {
            event.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
            resumeEventMongoRepository.save(event);
        }

        return new ResumeVersionDto(
                resume.getId(),
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

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resume.rename(name);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResumeEditResponse edit(Long userId, Long resumeId, ResumeEditRequest req) {
        String message = (req == null || req.message() == null) ? "" : req.message().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        ResumeEditTransactionService.EditPrepared prepared =
                resumeEditTransactionService.prepareEdit(userId, resumeId);

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
            throw e;
        } catch (Exception e) {
            markEditFailedAndLog(userId, resumeId, prepared, e.getMessage());
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

    public void saveVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeEventDocument event =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resume.getId(), versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (event.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        event.markCommitted(Instant.now());
        resumeEventMongoRepository.save(event);
        resume.setCurrentVersionNo(versionNo);
    }

    public void delete(Long userId, Long resumeId) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resumeEventMongoRepository.deleteByResumeId(resume.getId());
        resumeRepository.delete(resume);
    }
}
