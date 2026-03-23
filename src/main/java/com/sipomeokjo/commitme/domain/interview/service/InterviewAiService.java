package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private static final String AI_SERVICE = "aiService";

    private final RestClient aiClient;
    private final AiProperties aiProperties;

    @CircuitBreaker(name = AI_SERVICE, fallbackMethod = "generateInterviewFallback")
    @Retry(name = AI_SERVICE)
    public AiInterviewGenerateResponse generateInterview(AiInterviewGenerateRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewStartPath();
        try {
            AiInterviewGenerateResponse response =
                    aiClient.post()
                            .uri(url)
                            .body(request)
                            .retrieve()
                            .body(AiInterviewGenerateResponse.class);
            log.info("[AI_INTERVIEW] generate_success");
            return response;
        } catch (ResourceAccessException e) {
            log.error("[AI_INTERVIEW] generate_failed reason=connection_error", e);
            throw handleResourceAccessException(e);
        } catch (RestClientException e) {
            log.error("[AI_INTERVIEW] generate_failed reason=rest_client_error", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @CircuitBreaker(name = AI_SERVICE, fallbackMethod = "chatInterviewFallback")
    @Retry(name = AI_SERVICE)
    public AiInterviewChatResponse chatInterview(AiInterviewChatRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewAnswerPath();
        try {
            AiInterviewChatResponse response =
                    aiClient.post()
                            .uri(url)
                            .body(request)
                            .retrieve()
                            .body(AiInterviewChatResponse.class);
            log.info("[AI_INTERVIEW] chat_success");
            return response;
        } catch (ResourceAccessException e) {
            log.error("[AI_INTERVIEW] chat_failed reason=connection_error", e);
            throw handleResourceAccessException(e);
        } catch (RestClientException e) {
            log.error("[AI_INTERVIEW] chat_failed reason=rest_client_error", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @CircuitBreaker(name = AI_SERVICE, fallbackMethod = "endInterviewFallback")
    @Retry(name = AI_SERVICE)
    public AiInterviewEndResponse endInterview(AiInterviewEndRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewEndPath();
        try {
            AiInterviewEndResponse response =
                    aiClient.post()
                            .uri(url)
                            .body(request)
                            .retrieve()
                            .body(AiInterviewEndResponse.class);
            log.info("[AI_INTERVIEW] end_success");
            return response;
        } catch (ResourceAccessException e) {
            log.error("[AI_INTERVIEW] end_failed reason=connection_error", e);
            throw handleResourceAccessException(e);
        } catch (RestClientException e) {
            log.error("[AI_INTERVIEW] end_failed reason=rest_client_error", e);
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

    private AiInterviewGenerateResponse generateInterviewFallback(
            AiInterviewGenerateRequest request, CallNotPermittedException e) {
        log.warn("[AI_INTERVIEW] circuit_breaker_open action=generate");
        throw new BusinessException(ErrorCode.AI_CIRCUIT_BREAKER_OPEN);
    }

    private AiInterviewGenerateResponse generateInterviewFallback(
            AiInterviewGenerateRequest request, Exception e) {
        log.error("[AI_INTERVIEW] fallback_triggered action=generate", e);
        if (e instanceof BusinessException) {
            throw (BusinessException) e;
        }
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    private AiInterviewChatResponse chatInterviewFallback(
            AiInterviewChatRequest request, CallNotPermittedException e) {
        log.warn("[AI_INTERVIEW] circuit_breaker_open action=chat");
        throw new BusinessException(ErrorCode.AI_CIRCUIT_BREAKER_OPEN);
    }

    private AiInterviewChatResponse chatInterviewFallback(
            AiInterviewChatRequest request, Exception e) {
        log.error("[AI_INTERVIEW] fallback_triggered action=chat", e);
        if (e instanceof BusinessException) {
            throw (BusinessException) e;
        }
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    private AiInterviewEndResponse endInterviewFallback(
            AiInterviewEndRequest request, CallNotPermittedException e) {
        log.warn("[AI_INTERVIEW] circuit_breaker_open action=end");
        throw new BusinessException(ErrorCode.AI_CIRCUIT_BREAKER_OPEN);
    }

    private AiInterviewEndResponse endInterviewFallback(
            AiInterviewEndRequest request, Exception e) {
        log.error("[AI_INTERVIEW] fallback_triggered action=end", e);
        if (e instanceof BusinessException) {
            throw (BusinessException) e;
        }
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }
}
