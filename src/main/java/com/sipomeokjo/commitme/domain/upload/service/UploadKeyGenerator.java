package com.sipomeokjo.commitme.domain.upload.service;

import com.sipomeokjo.commitme.domain.upload.entity.UploadPurpose;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UploadKeyGenerator {

    public String generate(UploadPurpose purpose, String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        String uuid = UUID.randomUUID().toString();
        String purposePrefix = resolvePurposePrefix(purpose);
        return "uploads/" + purposePrefix + "/" + uuid + "_" + safeFileName;
    }

    private String resolvePurposePrefix(UploadPurpose purpose) {
        if (purpose == null) {
            return "unknown";
        }
        return switch (purpose) {
            case CHAT_ATTACHMENT -> "chat";
            case PROFILE_IMAGE -> "profile";
            case INTERVIEW_AUDIO -> "interview";
        };
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replace(" ", "_").replace("/", "_").replace("\\", "_");
    }
}
