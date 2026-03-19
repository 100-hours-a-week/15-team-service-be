package com.sipomeokjo.commitme.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OutboxCleanupProperties.class, PartitionManagementProperties.class})
public class AppBatchPropertiesConfig {}
