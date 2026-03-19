package com.sipomeokjo.commitme.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch.partition-management")
public record PartitionManagementProperties(
        boolean enabled,
        int monthsAhead,
        int monthsToKeep,
        String cron,
        Duration lockAtMost,
        Duration lockAtLeast) {}
