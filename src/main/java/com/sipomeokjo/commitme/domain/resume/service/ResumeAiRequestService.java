package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeEditRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeEditResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateResponse;
import com.sipomeokjo.commitme.security.jwt.AccessTokenCipher;
import java.net.SocketTimeoutException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAiRequestService {

    private final AuthRepository authRepository;
    private final AccessTokenCipher accessTokenCipher;
    private final RestClient aiClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public DispatchResult requestGenerateJob(
            Long userId, String positionName, List<String> repoUrls) {
        if (userId == null || positionName == null || positionName.isBlank()) {
            return DispatchResult.failed("AI_GENERATE_FAILED", "invalid request context");
        }

        if (repoUrls == null || repoUrls.isEmpty()) {
            return DispatchResult.failed("AI_GENERATE_FAILED", "repoUrls is empty");
        }

        String githubToken;
        try {
            var auth =
                    authRepository
                            .findByUser_IdAndProvider(userId, AuthProvider.GITHUB)
                            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            String rawToken = auth.getAccessToken();
            if (rawToken == null || rawToken.isBlank()) {
                log.debug("[AI_RESUME] no_github_token userId={}", userId);
                return DispatchResult.failed("NO_GITHUB_TOKEN", "github token not available");
            }
            githubToken = accessTokenCipher.decrypt(rawToken);
        } catch (BusinessException e) {
            return DispatchResult.failed("AI_GENERATE_FAILED", e.getMessage());
        } catch (Exception e) {
            log.error("[AI_RESUME] token_decrypt_failed", e);
            return DispatchResult.failed("AI_GENERATE_FAILED", "failed to get github token");
        }

        AiResumeGenerateRequest aiReq =
                new AiResumeGenerateRequest(repoUrls, positionName, githubToken);

        try {
            String url = aiProperties.getBaseUrl() + aiProperties.getResumeGeneratePath();

            AiResumeGenerateResponse aiRes =
                    aiClient.post()
                            .uri(url)
                            .body(aiReq)
                            .retrieve()
                            .body(AiResumeGenerateResponse.class);

            if (aiRes == null || aiRes.jobId() == null || aiRes.jobId().isBlank()) {
                log.warn("[AI_RESUME] generate_invalid_response");
                return DispatchResult.failed("AI_RESPONSE_INVALID", "jobId is null/blank");
            }

            log.info("[AI_RESUME] generate_success jobId={}", aiRes.jobId());
            return DispatchResult.succeeded(aiRes.jobId());
        } catch (ResourceAccessException e) {
            log.error("[AI_RESUME] generate_failed reason=connection_error", e);
            String errorMsg = getConnectionErrorMessage(e);
            throw new RuntimeException(errorMsg, e);
        } catch (RestClientException e) {
            log.error("[AI_RESUME] generate_failed reason=rest_client_error", e);
            throw new RuntimeException("AI server communication error", e);
        }
    }

    private String getConnectionErrorMessage(ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
            return "AI server response timeout";
        }
        return "AI server connection failed";
    }

    public String requestEdit(Long resumeId, String resumeJson, String requestMessage) {
        if (resumeJson == null || resumeJson.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (requestMessage == null || requestMessage.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        try {
            AiResumeEditRequest aiReq =
                    new AiResumeEditRequest(
                            resumeId, objectMapper.readTree(resumeJson), requestMessage);

            AiResumeEditResponse aiRes =
                    aiClient.post()
                            .uri(aiProperties.getBaseUrl() + aiProperties.getResumeEditPath())
                            .body(aiReq)
                            .retrieve()
                            .body(AiResumeEditResponse.class);

            if (aiRes == null || aiRes.jobId() == null || aiRes.jobId().isBlank()) {
                log.warn("[AI_EDIT] invalid_response");
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            log.info("[AI_EDIT] success jobId={}", aiRes.jobId());
            return aiRes.jobId();
        } catch (BusinessException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("[AI_EDIT] failed reason=connection_error", e);
            throw handleResourceAccessException(e);
        } catch (RestClientException e) {
            log.error("[AI_EDIT] failed reason=rest_client_error", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("[AI_EDIT] failed reason=unexpected_error", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException handleResourceAccessException(ResourceAccessException e) {
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
            return new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
        }
        return new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    public record DispatchResult(
            boolean success, String jobId, String errorCode, String errorMessage) {
        public static DispatchResult succeeded(String jobId) {
            return new DispatchResult(true, jobId, null, null);
        }

        public static DispatchResult failed(String errorCode, String errorMessage) {
            return new DispatchResult(false, null, errorCode, errorMessage);
        }
    }
}
