package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewAnswerRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewStartRequest;
import com.sipomeokjo.commitme.domain.interview.entity.AnswerInputType;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final RestClient aiClient;
    private final AiProperties aiProperties;

    @Async
    public void sendStartRequest(Interview interview, String resumeContent) {
        AiInterviewStartRequest request =
                new AiInterviewStartRequest(
                        interview.getAiSessionId(),
                        interview.getInterviewType(),
                        interview.getPosition().getName(),
                        interview.getCompany() != null ? interview.getCompany().getName() : null,
                        resumeContent,
                        aiProperties.getInterviewCallbackUrl());

        try {
            String url = aiProperties.getBaseUrl() + aiProperties.getInterviewStartPath();
            aiClient.post().uri(url).body(request).retrieve().toBodilessEntity();
            log.info("Interview start request sent: {}", interview.getAiSessionId());
        } catch (Exception e) {
            log.error("Failed to send interview start request: {}", interview.getAiSessionId(), e);
        }
    }

    @Async
    public void sendAnswerRequest(
            Interview interview,
            Integer turnNo,
            String answer,
            AnswerInputType answerInputType,
            String audioUrl) {
        AiInterviewAnswerRequest request =
                new AiInterviewAnswerRequest(
                        interview.getAiSessionId(),
                        turnNo,
                        answer,
                        answerInputType,
                        audioUrl,
                        Instant.now());

        try {
            String url = aiProperties.getBaseUrl() + aiProperties.getInterviewAnswerPath();
            aiClient.post().uri(url).body(request).retrieve().toBodilessEntity();
            log.info(
                    "Interview answer request sent: {}, turnNo: {}",
                    interview.getAiSessionId(),
                    turnNo);
        } catch (Exception e) {
            log.error("Failed to send interview answer request: {}", interview.getAiSessionId(), e);
        }
    }

    @Async
    public void sendEndRequest(Interview interview, List<InterviewMessage> messages) {
        List<AiInterviewEndRequest.MessagePayload> messagePayloads =
                messages.stream()
                        .map(
                                m ->
                                        new AiInterviewEndRequest.MessagePayload(
                                                m.getTurnNo(),
                                                m.getQuestion(),
                                                m.getAnswer(),
                                                m.getAnswerInputType(),
                                                m.getAudioUrl(),
                                                m.getAskedAt(),
                                                m.getAnsweredAt()))
                        .toList();

        AiInterviewEndRequest request =
                new AiInterviewEndRequest(
                        interview.getAiSessionId(),
                        interview.getInterviewType(),
                        interview.getPosition().getName(),
                        interview.getCompany() != null ? interview.getCompany().getName() : null,
                        messagePayloads);

        try {
            String url = aiProperties.getBaseUrl() + aiProperties.getInterviewEndPath();
            aiClient.post().uri(url).body(request).retrieve().toBodilessEntity();
            log.info("Interview end request sent: {}", interview.getAiSessionId());
        } catch (Exception e) {
            log.error("Failed to send interview end request: {}", interview.getAiSessionId(), e);
        }
    }
}
