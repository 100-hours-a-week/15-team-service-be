package com.sipomeokjo.commitme.domain.stt.service;

import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.stt.dto.ai.AiSttTranscribeRequest;
import com.sipomeokjo.commitme.domain.stt.dto.ai.AiSttTranscribeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttAiService {

    private final RestClient aiClient;
    private final AiProperties aiProperties;

    public AiSttTranscribeResponse transcribe(AiSttTranscribeRequest request) {
        String url = aiProperties.getBaseUrl() + aiProperties.getSttTranscribePath();
        AiSttTranscribeResponse response =
                aiClient.post()
                        .uri(url)
                        .body(request)
                        .retrieve()
                        .body(AiSttTranscribeResponse.class);
        log.info("STT transcribe request sent");
        return response;
    }
}
