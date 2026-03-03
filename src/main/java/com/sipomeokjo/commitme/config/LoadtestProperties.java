package com.sipomeokjo.commitme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.loadtest")
public class LoadtestProperties {

    private MockAuth mockAuth = new MockAuth();
    private MockAi mockAi = new MockAi();

    public String resolveResumeGeneratePath() {
        return StringUtils.hasText(mockAi.getResumeGeneratePath())
                ? mockAi.getResumeGeneratePath()
                : null;
    }

    @Getter
    @Setter
    public static class MockAuth {
        private boolean enabled = false;
        private int maxBulkCount = 500;
        private Long defaultPositionId;
    }

    @Getter
    @Setter
    public static class MockAi {
        private String resumeGeneratePath;
    }
}
