package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProjectionRebuildService {

    private static final int REBUILD_BATCH_SIZE = 100;

    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeMongoQueryRepository resumeMongoQueryRepository;

    public void rebuildForResume(Long resumeId) {
        ResumeDocument existing = resumeMongoRepository.findByResumeId(resumeId).orElse(null);
        if (existing == null) {
            log.warn("[REBUILD] projection_not_found resumeId={}", resumeId);
            return;
        }

        List<ResumeEventDocument> events =
                resumeEventMongoRepository.findByResumeIdOrderByVersionNoAsc(resumeId);

        ResumeDocument rebuilt = ResumeDocument.rebuildFrom(existing, events);

        resumeMongoRepository.save(rebuilt);

        log.info(
                "[REBUILD] completed resumeId={} lastAppliedVersionNo={}",
                resumeId,
                rebuilt.getLastAppliedVersionNo());
    }

    public void rebuildAll() {
        int skip = 0;
        int total = 0;
        int success = 0;
        int failure = 0;

        while (true) {
            List<Long> batch =
                    resumeMongoQueryRepository.findAllResumeIdsBatch(skip, REBUILD_BATCH_SIZE);
            if (batch.isEmpty()) {
                break;
            }
            log.info("[REBUILD] processing_batch skip={} size={}", skip, batch.size());
            for (Long resumeId : batch) {
                try {
                    rebuildForResume(resumeId);
                    success++;
                } catch (Exception e) {
                    log.error("[REBUILD] failed resumeId={} error={}", resumeId, e.getMessage());
                    failure++;
                }
                total++;
            }
            skip += batch.size();
        }

        log.info("[REBUILD] all_done total={} success={} failure={}", total, success, failure);
    }
}
