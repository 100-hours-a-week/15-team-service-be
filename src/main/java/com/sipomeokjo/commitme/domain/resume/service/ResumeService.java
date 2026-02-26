package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.PagingResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeAiGenerateEvent;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final UserRepository userRepository;

    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagingResponse<ResumeSummaryDto> list(Long userId, int page, int size) {

        PageRequest pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Resume> result = resumeRepository.findSucceededByUserId(userId, pageable);

        List<Resume> resumes = result.getContent();
        List<ResumeSummaryDto> items = new ArrayList<>(resumes.size());

        for (Resume r : resumes) {
            Long positionId = null;
            String positionName = null;
            if (r.getPosition() != null) {
                positionId = r.getPosition().getId();
                positionName = r.getPosition().getName();
            }

            Long companyId = null;
            String companyName = null;
            if (r.getCompany() != null) {
                companyId = r.getCompany().getId();
                companyName = r.getCompany().getName();
            }

            items.add(
                    new ResumeSummaryDto(
                            r.getId(),
                            r.getName(),
                            positionId,
                            positionName,
                            companyId,
                            companyName,
                            r.getCurrentVersionNo(),
                            r.getUpdatedAt()));
        }

        PagingResponse.PageMeta meta =
                new PagingResponse.PageMeta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.hasNext());

        return new PagingResponse<>(items, meta);
    }

    public Long create(Long userId, ResumeCreateRequest req) {

        User user =
                userRepository
                        .findByIdWithLock(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<ResumeVersion> pendingVersions =
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

        Long positionId = null;
        String positionName = null;
        if (resume.getPosition() != null) {
            positionId = resume.getPosition().getId();
            positionName = resume.getPosition().getName();
        }

        Long companyId = null;
        String companyName = null;
        if (resume.getCompany() != null) {
            companyId = resume.getCompany().getId();
            companyName = resume.getCompany().getName();
        }

        return new ResumeDetailDto(
                resume.getId(),
                resume.getName(),
                positionId,
                positionName,
                companyId,
                companyName,
                resume.getCurrentVersionNo(),
                version.getContent(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
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
