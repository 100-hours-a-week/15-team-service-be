package com.sipomeokjo.commitme.domain.resume.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "resume_version")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResumeVersionStatus status;

    @Column(name = "content", columnDefinition = "json", nullable = false)
    private String content;

    @Column(name = "ai_task_id", length = 36)
    private String aiTaskId;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "preview_shown_at")
    private Instant previewShownAt;

    public static ResumeVersion createV1(Resume resume, String content) {
        ResumeVersion v = new ResumeVersion();
        v.resume = resume;
        v.versionNo = 1;
        v.status = ResumeVersionStatus.QUEUED;

        if (content == null || content.isBlank()) {
            v.content = "{}";
        } else {
            v.content = content;
        }

        return v;
    }

    public static ResumeVersion createNext(Resume resume, int versionNo, String content) {
        ResumeVersion v = new ResumeVersion();
        v.resume = resume;
        v.versionNo = versionNo;
        v.status = ResumeVersionStatus.QUEUED;

        if (content == null || content.isBlank()) {
            v.content = "{}";
        } else {
            v.content = content;
        }

        return v;
    }

    public void commitNow() {
        this.committedAt = Instant.now();
    }

    public void markPreviewShownNow() {
        if (this.previewShownAt == null) {
            this.previewShownAt = Instant.now();
        }
    }

    public void markQueued() {
        this.status = ResumeVersionStatus.QUEUED;
        this.startedAt = null;
        this.finishedAt = null;
        this.aiTaskId = null;
        this.errorLog = null;
    }

    public void startProcessing(String aiTaskId) {
        this.status = ResumeVersionStatus.PROCESSING;
        this.aiTaskId = aiTaskId;
        this.startedAt = Instant.now();
        this.finishedAt = null;
        this.errorLog = null;
    }

    public void succeed(String contentJson) {
        this.status = ResumeVersionStatus.SUCCEEDED;
        this.finishedAt = Instant.now();
        this.errorLog = null;

        if (contentJson == null || contentJson.isBlank()) {
            this.content = "{}";
        } else {
            this.content = contentJson;
        }

        if (this.resume != null) {
            this.resume.touchUpdatedAtNow();
        }
    }

    public void failNow(String errorCode, String message) {
        this.status = ResumeVersionStatus.FAILED;
        this.finishedAt = Instant.now();
        this.errorLog = "[" + errorCode + "] " + (message == null ? "" : message);
    }

    public boolean isProcessingTimedOut(long timeoutMinutes) {
        if (this.status != ResumeVersionStatus.PROCESSING) {
            return false;
        }
        if (this.startedAt == null) {
            return false;
        }
        return this.startedAt.plus(Duration.ofMinutes(timeoutMinutes)).isBefore(Instant.now());
    }
}
