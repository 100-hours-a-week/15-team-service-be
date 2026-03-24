package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeFinder {

    private final ResumeMongoRepository resumeMongoRepository;

    public ResumeDocument getDocumentByResumeIdAndUserIdOrThrow(Long resumeId, Long userId) {
        ResumeDocument doc =
                resumeMongoRepository
                        .findByResumeId(resumeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
        return doc;
    }
}
