package com.sipomeokjo.commitme.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.loadtest.mock-auth")
public class LoadtestMockAuthProperties {

    private boolean enabled = false;
    private int maxBulkCount = 500;
    private Long defaultPositionId;
}
