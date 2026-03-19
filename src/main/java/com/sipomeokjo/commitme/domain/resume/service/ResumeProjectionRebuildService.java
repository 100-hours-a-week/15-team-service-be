package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProjectionRebuildService {

    private final ResumeRepository resumeRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeMongoRepository resumeMongoRepository;

    /**
     * Replays all resume_events for a single resume and overwrites the projection document. Uses
     * default (JPA) transaction for reading the Resume entity with lazy fields.
     */
    @Transactional(readOnly = true)
    public void rebuildForResume(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null) {
            log.warn("[REBUILD] resume_not_found resumeId={}", resumeId);
            return;
        }

        // Access lazy fields within the JPA transaction
        if (resume.getPosition() != null) resume.getPosition().getName();
        if (resume.getCompany() != null) resume.getCompany().getName();
        if (resume.getUser() != null) resume.getUser().getId();

        List<ResumeEventDocument> events =
                resumeEventMongoRepository.findByResumeIdOrderByVersionNoAsc(resumeId);

        ResumeDocument rebuilt = ResumeDocument.rebuildFrom(resume, events);

        // Delete existing and save rebuilt (upsert semantics)
        resumeMongoRepository.deleteByResumeId(resumeId);
        resumeMongoRepository.save(rebuilt);

        log.info(
                "[REBUILD] completed resumeId={} lastAppliedVersionNo={}",
                resumeId,
                rebuilt.getLastAppliedVersionNo());
    }

    /** Rebuilds projection documents for all resumes sequentially. */
    public void rebuildAll() {
        List<Long> resumeIds = resumeRepository.findAll().stream().map(Resume::getId).toList();
        log.info("[REBUILD] starting_all count={}", resumeIds.size());
        int success = 0;
        int failure = 0;
        for (Long resumeId : resumeIds) {
            try {
                rebuildForResume(resumeId);
                success++;
            } catch (Exception e) {
                log.error("[REBUILD] failed resumeId={} error={}", resumeId, e.getMessage());
                failure++;
            }
        }
        log.info(
                "[REBUILD] all_done total={} success={} failure={}",
                resumeIds.size(),
                success,
                failure);
    }
}
