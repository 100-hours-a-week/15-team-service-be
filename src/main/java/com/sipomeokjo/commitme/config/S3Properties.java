package com.sipomeokjo.commitme.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        String cdnBaseUrl,
        int presignDurationMinutes) {

    public S3Properties {
        if (presignDurationMinutes <= 0) {
            presignDurationMinutes = 30;
        }
    }
}
