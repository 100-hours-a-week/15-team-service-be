package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeAiCallbackService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final ObjectMapper objectMapper;

    public void handleCallback(AiResumeCallbackRequest req) {

        if (req == null || req.jobId() == null || req.jobId().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (req.status() == null || req.status().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        ResumeVersion version = resumeVersionRepository.findByAiTaskId(req.jobId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        // 이미 처리된 콜백이면 무시(재전송 대비)
        if (version.getStatus() == ResumeVersionStatus.SUCCEEDED ||
                version.getStatus() == ResumeVersionStatus.FAILED) {
            return;
        }

        String status = req.status().trim();

        if ("success".equalsIgnoreCase(status)) {
            if (req.resume() == null) {
                version.failNow("BAD_CALLBACK", "resume is null");
                return;
            }

            try {
                // resume payload → JSON string (DB json 컬럼에 저장)
                String json = objectMapper.writeValueAsString(req.resume());
                version.succeed(json);
            } catch (Exception e) {
                version.failNow("JSON_SERIALIZATION_FAILED", e.getMessage());
            }
            return;
        }

        if ("failed".equalsIgnoreCase(status)) {
            String code = (req.error() == null || req.error().code() == null || req.error().code().isBlank())
                    ? "AI_FAILED"
                    : req.error().code();
            String msg = (req.error() == null || req.error().message() == null)
                    ? "unknown"
                    : req.error().message();

            version.failNow(code, msg);
            return;
        }

        version.failNow("BAD_CALLBACK", "invalid status=" + req.status());
    }
}
