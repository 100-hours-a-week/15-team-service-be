package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateResponse;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeAiGenerateEvent;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.security.jwt.AccessTokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class ResumeAiRequestService {

    private final ResumeVersionRepository resumeVersionRepository;
    private final AuthRepository authRepository;
    private final AccessTokenCipher accessTokenCipher;
    private final RestClient aiClient;
    private final AiProperties aiProperties;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGenerate(ResumeAiGenerateEvent event) {
        if (event == null || event.resumeVersionId() == null) {
            return;
        }

        ResumeVersion version =
                resumeVersionRepository.findById(event.resumeVersionId()).orElse(null);

        if (version == null || version.getStatus() != ResumeVersionStatus.QUEUED) {
            return;
        }

        String githubToken;
        try {
            var auth =
                    authRepository
                            .findByUser_IdAndProvider(event.userId(), AuthProvider.GITHUB)
                            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            githubToken = accessTokenCipher.decrypt(auth.getAccessToken());
        } catch (Exception e) {
            version.failNow("AI_GENERATE_FAILED", e.getMessage());
            return;
        }

        AiResumeGenerateRequest aiReq =
                new AiResumeGenerateRequest(event.repoUrls(), event.positionName(), githubToken);

        try {
            String url = aiProperties.getBaseUrl() + aiProperties.getResumeGeneratePath();

            AiResumeGenerateResponse aiRes =
                    aiClient.post()
                            .uri(url)
                            .body(aiReq)
                            .retrieve()
                            .body(AiResumeGenerateResponse.class);

            if (aiRes == null || aiRes.jobId() == null || aiRes.jobId().isBlank()) {
                version.failNow("AI_RESPONSE_INVALID", "jobId is null/blank");
                return;
            }

            version.startProcessing(aiRes.jobId());
        } catch (Exception e) {
            version.failNow("AI_GENERATE_FAILED", e.getMessage());
        }
    }
}
