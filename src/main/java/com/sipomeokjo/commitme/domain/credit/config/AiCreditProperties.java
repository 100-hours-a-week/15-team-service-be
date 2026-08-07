package com.sipomeokjo.commitme.domain.credit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-credit")
public class AiCreditProperties {
    private long initialCredit = 100L;
    private long interviewStartCost = 20L;
    private long resumeGenerateCost = 30L;
    private long resumeEditCost = 3L;
}
