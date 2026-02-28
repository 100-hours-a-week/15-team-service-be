package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeEditTransactionService {
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;

    @Transactional
    public EditPrepared prepareEdit(Long userId, Long resumeId) {
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

        ResumeVersion latestSucceeded =
                resumeVersionRepository
                        .findTopByResume_IdAndStatusOrderByVersionNoDesc(
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
        ResumeVersion saved = resumeVersionRepository.save(next);
        return new EditPrepared(
                saved.getId(),
                resume.getId(),
                resume.getName(),
                saved.getVersionNo(),
                saved.getContent());
    }

    @Transactional
    public ResumeVersion markEditRequested(Long resumeVersionId, String jobId) {
        ResumeVersion next =
                resumeVersionRepository
                        .findById(resumeVersionId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        next.startProcessing(jobId);
        return next;
    }

    @Transactional
    public void markEditFailed(Long resumeVersionId, String errorMessage) {
        ResumeVersion next =
                resumeVersionRepository
                        .findById(resumeVersionId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        next.failNow("AI_EDIT_FAILED", errorMessage);
    }

    public record EditPrepared(
            Long resumeVersionId,
            Long resumeId,
            String resumeName,
            Integer versionNo,
            String baseContent) {}
}
