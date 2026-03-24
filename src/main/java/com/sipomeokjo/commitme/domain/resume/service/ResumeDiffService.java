package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDiffDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDiffDto.DiffItem;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeDiffService {

    private static final String BASE_CURRENT = "current";

    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ResumeVersionDiffDto getDiff(
            Long userId, Long resumeId, int targetVersionNo, String baseParam) {

        int baseVersionNo = resolveBaseVersionNo(userId, resumeId, baseParam);

        ResumeEventDocument targetEvent =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, targetVersionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        if (targetEvent.getStatus() != ResumeVersionStatus.SUCCEEDED
                || targetEvent.getCommittedAt() == null) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        Map<String, Object> targetSnapshot =
                parseSnapshot(targetEvent.getSnapshot(), resumeId, targetVersionNo);

        if (baseVersionNo == targetVersionNo) {
            return new ResumeVersionDiffDto(resumeId, baseVersionNo, targetVersionNo, List.of());
        }

        ResumeEventDocument baseEvent =
                resumeEventMongoRepository
                        .findByResumeIdAndVersionNo(resumeId, baseVersionNo)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));
        if (baseEvent.getStatus() != ResumeVersionStatus.SUCCEEDED
                || baseEvent.getCommittedAt() == null) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        Map<String, Object> baseSnapshot =
                parseSnapshot(baseEvent.getSnapshot(), resumeId, baseVersionNo);

        ResumeDiffEngine engine = new ResumeDiffEngine();
        List<ResumeDiffEngine.DiffEntry> entries = engine.diff(baseSnapshot, targetSnapshot);
        List<DiffItem> diffs =
                entries.stream()
                        .map(
                                e ->
                                        new DiffItem(
                                                e.path(),
                                                e.changeType().name(),
                                                e.before(),
                                                e.after()))
                        .toList();

        return new ResumeVersionDiffDto(resumeId, baseVersionNo, targetVersionNo, diffs);
    }

    private int resolveBaseVersionNo(Long userId, Long resumeId, String baseParam) {
        if (BASE_CURRENT.equalsIgnoreCase(baseParam) || baseParam == null || baseParam.isBlank()) {
            ResumeDocument doc =
                    resumeMongoRepository
                            .findByResumeId(resumeId)
                            .filter(d -> d.getUserId().equals(userId))
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
            if (doc.getCurrentVersionNo() == null) {
                throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND);
            }
            return doc.getCurrentVersionNo();
        }

        try {
            int vno = Integer.parseInt(baseParam.trim());
            resumeMongoRepository
                    .findByResumeId(resumeId)
                    .filter(doc -> doc.getUserId().equals(userId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
            return vno;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private Map<String, Object> parseSnapshot(String snapshot, Long resumeId, int versionNo) {
        if (snapshot == null || snapshot.isBlank() || "{}".equals(snapshot)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshot, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn(
                    "[DIFF] snapshot_parse_failed resumeId={} versionNo={} error={}",
                    resumeId,
                    versionNo,
                    e.getMessage());
            return Map.of();
        }
    }
}
