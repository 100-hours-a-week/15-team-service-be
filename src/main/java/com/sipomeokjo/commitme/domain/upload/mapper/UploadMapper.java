package com.sipomeokjo.commitme.domain.upload.mapper;

import com.sipomeokjo.commitme.domain.upload.dto.UploadConfirmResponse;
import com.sipomeokjo.commitme.domain.upload.dto.UploadCreateResponse;
import com.sipomeokjo.commitme.domain.upload.entity.Upload;
import org.springframework.stereotype.Component;

@Component
public class UploadMapper {

    public UploadCreateResponse toUploadCreateResponse(Upload upload, String presignedUrl) {
        if (upload == null) {
            return null;
        }
        return new UploadCreateResponse(
                upload.getId(), presignedUrl, upload.getS3Key(), upload.getExpiresAt());
    }

    public UploadConfirmResponse toUploadConfirmResponse(Upload upload) {
        if (upload == null) {
            return null;
        }
        return new UploadConfirmResponse(
                upload.getId(), upload.getId(), upload.getS3Key(), upload.getUploadedAt());
    }
}
