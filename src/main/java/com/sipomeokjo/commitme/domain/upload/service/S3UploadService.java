package com.sipomeokjo.commitme.domain.upload.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.S3Properties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public PresignResult presignPutObject(String s3Key, String contentType) {
        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(s3Key)
                            .contentType(contentType)
                            .build();

            Duration signatureDuration = Duration.ofMinutes(s3Properties.presignDurationMinutes());
            PutObjectPresignRequest presignRequest =
                    PutObjectPresignRequest.builder()
                            .signatureDuration(signatureDuration)
                            .putObjectRequest(putObjectRequest)
                            .build();

            PresignedPutObjectRequest presignedRequest =
                    s3Presigner.presignPutObject(presignRequest);
            return new PresignResult(presignedRequest.url().toString(), signatureDuration);
        } catch (SdkException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_S3_ERROR);
        }
    }

    public PresignResult presignGetObject(String s3Key) {
        try {
            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder().bucket(s3Properties.bucket()).key(s3Key).build();

            Duration signatureDuration = Duration.ofMinutes(s3Properties.presignDurationMinutes());
            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(signatureDuration)
                            .getObjectRequest(getObjectRequest)
                            .build();

            PresignedGetObjectRequest presignedRequest =
                    s3Presigner.presignGetObject(presignRequest);
            return new PresignResult(presignedRequest.url().toString(), signatureDuration);
        } catch (SdkException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_S3_ERROR);
        }
    }

    public String toCdnUrl(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isBlank()) {
            return s3KeyOrUrl;
        }
        String normalizedKey = normalizeKey(s3KeyOrUrl);
        if (isCdnUrl(normalizedKey)) {
            return normalizedKey;
        }
        if (normalizedKey.startsWith("http://") || normalizedKey.startsWith("https://")) {
            return normalizedKey;
        }
        String cdnBaseUrl = normalizeCdnBaseUrl();
        if (cdnBaseUrl == null || cdnBaseUrl.isBlank()) {
            return normalizedKey;
        }
        return cdnBaseUrl + "/" + stripLeadingSlash(normalizedKey);
    }

    public String toS3Key(String s3KeyOrUrl) {
        if (s3KeyOrUrl == null || s3KeyOrUrl.isBlank()) {
            return s3KeyOrUrl;
        }
        return normalizeKey(s3KeyOrUrl);
    }

    public HeadResult headObject(String s3Key) {
        try {
            HeadObjectRequest request =
                    HeadObjectRequest.builder().bucket(s3Properties.bucket()).key(s3Key).build();

            HeadObjectResponse response = s3Client.headObject(request);
            return new HeadResult(response.contentLength(), response.eTag());
        } catch (NoSuchKeyException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new BusinessException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.UPLOAD_S3_ERROR);
        } catch (SdkException ex) {
            throw new BusinessException(ErrorCode.UPLOAD_S3_ERROR);
        }
    }

    public record PresignResult(String presignedUrl, Duration signatureDuration) {}

    public record HeadResult(long contentLength, String eTag) {}

    private String normalizeKey(String s3KeyOrUrl) {
        if (!s3KeyOrUrl.startsWith("http://") && !s3KeyOrUrl.startsWith("https://")) {
            return s3KeyOrUrl;
        }
        String bucketPrefix = "https://" + s3Properties.bucket() + ".s3.";
        int bucketIndex = s3KeyOrUrl.indexOf(bucketPrefix);
        if (bucketIndex < 0) {
            return s3KeyOrUrl;
        }
        try {
            java.net.URI uri = java.net.URI.create(s3KeyOrUrl);
            String rawPath = uri.getRawPath();
            if (rawPath == null || rawPath.length() <= 1) {
                return s3KeyOrUrl;
            }
            return rawPath.substring(1);
        } catch (IllegalArgumentException ex) {
            return s3KeyOrUrl;
        }
    }

    private boolean isCdnUrl(String url) {
        String cdnBaseUrl = normalizeCdnBaseUrl();
        return cdnBaseUrl != null && !cdnBaseUrl.isBlank() && url.startsWith(cdnBaseUrl);
    }

    private String normalizeCdnBaseUrl() {
        String cdnBaseUrl = s3Properties.cdnBaseUrl();
        if (cdnBaseUrl == null || cdnBaseUrl.isBlank()) {
            return cdnBaseUrl;
        }
        if (cdnBaseUrl.endsWith("/")) {
            return cdnBaseUrl.substring(0, cdnBaseUrl.length() - 1);
        }
        return cdnBaseUrl;
    }

    private String stripLeadingSlash(String key) {
        if (key.startsWith("/")) {
            return key.substring(1);
        }
        return key;
    }
}
