package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeFinder {

    private final ResumeRepository resumeRepository;

    public Resume getByIdAndUserIdOrThrow(Long resumeId, Long userId) {
        return resumeRepository
                .findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    public Resume getByIdAndUserIdWithLockOrThrow(Long resumeId, Long userId) {
        return resumeRepository
                .findByIdAndUserIdWithLock(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }
}
