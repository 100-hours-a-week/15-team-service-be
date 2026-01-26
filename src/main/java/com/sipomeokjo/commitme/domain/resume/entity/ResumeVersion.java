package com.sipomeokjo.commitme.domain.resume.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    /**
     * ⚠️ JSON 컬럼에는 반드시 "유효한 JSON 문자열"만 들어가야 함
     * 예: "{}", "[]", {"repos":[]}
     */
    @Column(name = "content", columnDefinition = "json", nullable = false)
    private String content;

    @Column(name = "ai_task_id", length = 36)
    private String aiTaskId;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "committed_at")
    private LocalDateTime committedAt;

    /**
     * 최초 생성되는 v1
     */
    public static ResumeVersion createV1(Resume resume, String content) {
        ResumeVersion v = new ResumeVersion();
        v.resume = resume;
        v.versionNo = 1;
        v.status = ResumeVersionStatus.SUCCEEDED; // 정책상 v1 고정이면 OK

        // ✅ JSON 컬럼 안전 처리
        if (content == null || content.isBlank()) {
            v.content = "{}";   // ⭐ 핵심 포인트
        } else {
            v.content = content;
        }

        return v;
    }

    public void commitNow() {
        this.committedAt = LocalDateTime.now();
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
        this.startedAt = LocalDateTime.now();
        this.finishedAt = null;
        this.errorLog = null;
    }

    public void succeed(String contentJson) {
        this.status = ResumeVersionStatus.SUCCEEDED;
        this.finishedAt = LocalDateTime.now();
        this.errorLog = null;

        // JSON 컬럼 안전 처리
        if (contentJson == null || contentJson.isBlank()) {
            this.content = "{}";
        } else {
            this.content = contentJson;
        }
    }

    public void failNow(String errorCode, String message) {
        this.status = ResumeVersionStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        // error_log는 TEXT라서 그냥 문자열로 남겨도 OK
        this.errorLog = "[" + errorCode + "] " + (message == null ? "" : message);
    }



}
