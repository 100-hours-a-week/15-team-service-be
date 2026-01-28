package com.sipomeokjo.commitme.domain.upload.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;

@Getter
@Builder
@Entity
@Table(name = "uploads")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Upload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private UploadPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UploadStatus status;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "etag", length = 200)
    private String etag;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    public boolean isOwnedBy(Long userId) {
        return ownerUserId != null && ownerUserId.equals(userId);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now != null && now.isAfter(expiresAt);
    }

    public void markExpired(Instant now) {
        if (status == UploadStatus.PENDING && isExpired(now)) {
            this.status = UploadStatus.EXPIRED;
        }
    }

    public void confirmUploaded(String etag, Instant uploadedAt) {
        this.status = UploadStatus.UPLOADED;
        this.etag = etag;
        this.uploadedAt = uploadedAt;
    }
}
