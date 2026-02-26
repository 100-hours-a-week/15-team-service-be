package com.sipomeokjo.commitme.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.observability.jdbc")
public record JdbcMetricsProperties(Long slowQueryThresholdMs) {

    public JdbcMetricsProperties {
        if (slowQueryThresholdMs == null || slowQueryThresholdMs <= 0) {
            slowQueryThresholdMs = 300L;
        }
    }
}
