package com.sipomeokjo.commitme.domain.upload.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.S3Properties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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
}
