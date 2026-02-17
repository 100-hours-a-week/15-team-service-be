package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.pagination.CursorResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.validation.KeywordValidator;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeAiGenerateEvent;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeMapper;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final UserRepository userRepository;
    private final CursorParser cursorParser;
    private final ResumeMapper resumeMapper;

    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final ResumeAiRequestService resumeAiRequestService;

    @Transactional(readOnly = true)
    public CursorResponse<ResumeSummaryDto> list(
            Long userId, CursorRequest request, String keyword, String sortedBy) {
        ResumeSortBy sortBy = ResumeSortBy.from(sortedBy);
        CursorParser.Cursor cursor = cursorParser.parse(request == null ? null : request.next());
        int size = CursorRequest.resolveLimit(request, 10);
        String normalizedKeyword = KeywordValidator.normalize(keyword, 30);

        List<Resume> resumes =
                (sortBy == ResumeSortBy.UPDATED_ASC)
                        ? resumeRepository.findSucceededByUserIdWithCursorAsc(
                                userId,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                PageRequest.of(0, size + 1))
                        : resumeRepository.findSucceededByUserIdWithCursorDesc(
                                userId,
                                normalizedKeyword,
                                cursor.createdAt(),
                                cursor.id(),
                                PageRequest.of(0, size + 1));

        boolean hasMore = resumes.size() > size;
        List<Resume> pageResumes = hasMore ? resumes.subList(0, size) : resumes;
        List<ResumeSummaryDto> items =
                pageResumes.stream().map(resumeMapper::toSummaryDto).toList();

        String next =
                hasMore && !pageResumes.isEmpty() ? encodeCursor(pageResumes.getLast()) : null;
        return new CursorResponse<>(items, null, next);
    }

    private String encodeCursor(Resume resume) {
        return resume.getUpdatedAt() + "|" + resume.getId();
    }

    public Long create(Long userId, ResumeCreateRequest req) {

        User user =
                userRepository
                        .findByIdWithLock(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Long> pendingVersions =
                resumeVersionRepository.findByUserIdAndStatusIn(
                        userId,
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));
        if (!pendingVersions.isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_GENERATION_IN_PROGRESS);
        }

        if (req.getRepoUrls() == null || req.getRepoUrls().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (req.getPositionId() == null)
            throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
        Position position =
                positionRepository
                        .findById(req.getPositionId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));

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

        ResumeVersion v1 = ResumeVersion.createV1(saved, "{}");
        resumeVersionRepository.save(v1);

        ResumeAiGenerateEvent event =
                new ResumeAiGenerateEvent(
                        v1.getId(), userId, position.getName(), req.getRepoUrls());
        eventPublisher.publishEvent(event);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public ResumeDetailDto get(Long userId, Long resumeId) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion version =
                resumeVersionRepository
                        .findTopByResume_IdAndStatusOrderByVersionNoDesc(
                                resume.getId(), ResumeVersionStatus.SUCCEEDED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        return resumeMapper.toDetailDto(resume, version);
    }

    private static final long AI_PROCESSING_TIMEOUT_MINUTES = 5;

    public ResumeVersionDto getVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion v =
                resumeVersionRepository
                        .findByResume_IdAndVersionNo(resume.getId(), versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (v.isProcessingTimedOut(AI_PROCESSING_TIMEOUT_MINUTES)) {
            v.failNow("TIMEOUT", "AI 서버 응답 시간 초과");
        }

        return new ResumeVersionDto(
                resume.getId(),
                v.getVersionNo(),
                v.getStatus(),
                v.getContent(),
                v.getAiTaskId(),
                v.getErrorLog(),
                v.getStartedAt(),
                v.getFinishedAt(),
                v.getCommittedAt(),
                v.getCreatedAt(),
                v.getUpdatedAt());
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

    public ResumeEditResponse edit(Long userId, Long resumeId, ResumeEditRequest req) {
        String message = (req == null || req.getMessage() == null) ? "" : req.getMessage().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Resume resume =
                resumeRepository
                        .findByIdAndUserIdWithLock(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        List<Long> pendingVersions =
                resumeVersionRepository.findByResumeIdAndStatusInWithLock(
                        resume.getId(),
                        List.of(ResumeVersionStatus.QUEUED, ResumeVersionStatus.PROCESSING));
        if (!pendingVersions.isEmpty()) {
            log.warn(
                    "[RESUME_EDIT] in_progress userId={} resumeId={} pendingCount={}",
                    userId,
                    resumeId,
                    pendingVersions.size());
            throw new BusinessException(ErrorCode.RESUME_EDIT_IN_PROGRESS);
        }

        var latestSucceeded =
                resumeVersionRepository
                        .findLatestContentByResumeIdAndStatus(
                                resume.getId(), ResumeVersionStatus.SUCCEEDED)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        int nextVersionNo =
                resumeVersionRepository
                        .findLatestVersionNoByResumeId(resume.getId())
                        .map(v -> v.getVersionNo() + 1)
                        .orElse(1);

        ResumeVersion next =
                ResumeVersion.createNext(resume, nextVersionNo, latestSucceeded.getContent());
        resumeVersionRepository.save(next);

        try {
            String jobId = resumeAiRequestService.requestEdit(resumeId, next.getContent(), message);
            next.startProcessing(jobId);
            log.debug(
                    "[RESUME_EDIT] ai_requested userId={} resumeId={} versionNo={} taskId={}",
                    userId,
                    resumeId,
                    next.getVersionNo(),
                    jobId);
        } catch (BusinessException e) {
            next.failNow("AI_EDIT_FAILED", e.getMessage());
            log.warn(
                    "[RESUME_EDIT] ai_failed userId={} resumeId={} versionNo={} error={}",
                    userId,
                    resumeId,
                    next.getVersionNo(),
                    e.getMessage());
            throw e;
        } catch (Exception e) {
            next.failNow("AI_EDIT_FAILED", e.getMessage());
            log.warn(
                    "[RESUME_EDIT] ai_failed userId={} resumeId={} versionNo={} error={}",
                    userId,
                    resumeId,
                    next.getVersionNo(),
                    e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }

        return new ResumeEditResponse(
                resume.getId(),
                next.getVersionNo(),
                resume.getName(),
                next.getAiTaskId(),
                next.getUpdatedAt());
    }

    public void saveVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion v =
                resumeVersionRepository
                        .findByResume_IdAndVersionNo(resume.getId(), versionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (v.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        v.commitNow();
        resume.setCurrentVersionNo(versionNo);
    }

    public void delete(Long userId, Long resumeId) {

        Resume resume =
                resumeRepository
                        .findByIdAndUser_Id(resumeId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resumeVersionRepository.deleteByResume_Id(resume.getId());
        resumeRepository.delete(resume);
    }
}
