package com.sipomeokjo.commitme.domain.resume.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String baseUrl;
    private String resumeGeneratePath;
    private String resumeEditPath;
    private String resumeCallbackUrl;
    private String callbackSecret;

    // Interview
    private String interviewStartPath;
    private String interviewAnswerPath;
    private String interviewEndPath;
    private String interviewCallbackUrl;
}
