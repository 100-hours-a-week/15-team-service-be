package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final RestClient aiClient;
    private final AiProperties aiProperties;

    public AiInterviewGenerateResponse generateInterview(AiInterviewGenerateRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewStartPath();
        AiInterviewGenerateResponse response =
                aiClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(AiInterviewGenerateResponse.class);
        log.info("Interview generate request sent");
        return response;
    }

    public AiInterviewChatResponse chatInterview(AiInterviewChatRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewAnswerPath();
        AiInterviewChatResponse response =
                aiClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(AiInterviewChatResponse.class);
        log.info("Interview chat request sent");
        return response;
    }

    public AiInterviewEndResponse endInterview(AiInterviewEndRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getInterviewEndPath();
        AiInterviewEndResponse response =
                aiClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(AiInterviewEndResponse.class);
        log.info("Interview end request sent");
        return response;
    }
}
