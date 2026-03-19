package com.sipomeokjo.commitme.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch.outbox-cleanup")
public record OutboxCleanupProperties(
        boolean enabled,
        int retentionDays,
        int chunkSize,
        String cron,
        Duration lockAtMost,
        Duration lockAtLeast) {}
