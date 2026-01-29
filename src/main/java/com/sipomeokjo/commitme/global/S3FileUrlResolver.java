package com.sipomeokjo.commitme.global;

import com.sipomeokjo.commitme.config.S3Properties;
import org.springframework.stereotype.Component;

@Component
public class S3FileUrlResolver {

    private final S3Properties s3Properties;

    public S3FileUrlResolver(S3Properties s3Properties) {
        this.s3Properties = s3Properties;
    }

    public String toFileUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return s3Key;
        }
        if (s3Key.startsWith("http://") || s3Key.startsWith("https://")) {
            return s3Key;
        }
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.bucket(), s3Properties.region(), s3Key);
    }
}
