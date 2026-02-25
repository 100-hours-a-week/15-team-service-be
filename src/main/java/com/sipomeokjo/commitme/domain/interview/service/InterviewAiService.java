package com.sipomeokjo.commitme.domain.interview.service;

import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewChatResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewEndResponse;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateRequest;
import com.sipomeokjo.commitme.domain.interview.dto.ai.AiInterviewGenerateResponse;
import com.sipomeokjo.commitme.domain.interview.entity.AnswerInputType;
import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import com.sipomeokjo.commitme.domain.interview.entity.InterviewMessage;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import java.util.List;
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

    public AiInterviewEndResponse endInterview(
            Interview interview, List<InterviewMessage> messages) {
        List<AiInterviewEndRequest.MessagePayload> messagePayloads =
                messages.stream()
                        .map(
                                m ->
                                        new AiInterviewEndRequest.MessagePayload(
                                                m.getTurnNo(),
                                                m.getQuestion(),
                                                m.getAnswer(),
                                                toAiAnswerInputType(m.getAnswerInputType()),
                                                toIsoString(m.getAskedAt()),
                                                toIsoString(m.getAnsweredAt())))
                        .toList();

        String companyName =
                interview.getCompany() != null ? interview.getCompany().getName() : "미지정";

        AiInterviewEndRequest request =
                new AiInterviewEndRequest(
                        interview.getAiSessionId(),
                        interview.getInterviewType(),
                        interview.getPosition().getName(),
                        companyName,
                        messagePayloads);

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

    private String toAiAnswerInputType(AnswerInputType inputType) {
        if (inputType == null) {
            return "text";
        }
        return inputType == AnswerInputType.AUDIO ? "voice" : "text";
    }

    private String toIsoString(java.time.Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
