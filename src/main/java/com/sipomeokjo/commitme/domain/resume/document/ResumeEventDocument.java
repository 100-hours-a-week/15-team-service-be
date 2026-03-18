package com.sipomeokjo.commitme.domain.resume.document;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Document(collection = "resume_events")
@CompoundIndexes({
    @CompoundIndex(
            name = "ux_resume_events_resume_version",
            def = "{'resume_id': 1, 'version_no': 1}",
            unique = true),
    @CompoundIndex(
            name = "ux_resume_events_ai_task_id",
            def = "{'ai_task_id': 1}",
            unique = true,
            partialFilter = "{'ai_task_id': {'$exists': true}}"),
    @CompoundIndex(
            name = "ix_resume_events_preview_lookup",
            def =
                    "{'resume_id': 1, 'status': 1, 'committed_at': 1, 'preview_shown_at': 1, 'version_no': -1}"),
    @CompoundIndex(
            name = "ix_resume_events_pending_lookup",
            def = "{'status': 1, 'started_at': 1, 'created_at': 1}"),
    @CompoundIndex(
            name = "ix_resume_events_user_status_created",
            def = "{'user_id': 1, 'status': 1, 'created_at': 1}")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeEventDocument {

    @Id private String id;

    @Field("resume_id")
    private Long resumeId;

    @Field("version_no")
    private Integer versionNo;

    @Field("user_id")
    private Long userId;

    @Field("status")
    private ResumeVersionStatus status;

    @Field("snapshot")
    private String snapshot;

    @Field("ai_task_id")
    private String aiTaskId;

    @Field("error_log")
    private String errorLog;

    @Field("started_at")
    private Instant startedAt;

    @Field("finished_at")
    private Instant finishedAt;

    @Field("committed_at")
    private Instant committedAt;

    @Field("preview_shown_at")
    private Instant previewShownAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    public static ResumeEventDocument create(
            Long resumeId,
            Integer versionNo,
            Long userId,
            ResumeVersionStatus status,
            String snapshot) {
        ResumeEventDocument document = new ResumeEventDocument();
        document.resumeId = resumeId;
        document.versionNo = versionNo;
        document.userId = userId;
        document.status = status;
        document.snapshot = normalizeSnapshot(snapshot);
        return document;
    }

    public void startProcessing(String aiTaskId, Instant startedAt) {
        this.status = ResumeVersionStatus.PROCESSING;
        this.aiTaskId = aiTaskId;
        this.startedAt = startedAt;
        this.finishedAt = null;
        this.errorLog = null;
    }

    public void succeed(String snapshot, Instant finishedAt) {
        this.status = ResumeVersionStatus.SUCCEEDED;
        this.snapshot = normalizeSnapshot(snapshot);
        this.finishedAt = finishedAt;
        this.errorLog = null;
    }

    public void fail(String errorLog, Instant finishedAt) {
        this.status = ResumeVersionStatus.FAILED;
        this.finishedAt = finishedAt;
        this.errorLog = errorLog;
    }

    public void markCommitted(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public void markPreviewShown(Instant previewShownAt) {
        if (this.previewShownAt == null) {
            this.previewShownAt = previewShownAt;
        }
    }

    public void failNow(String errorCode, String message) {
        fail("[" + errorCode + "] " + (message == null ? "" : message), Instant.now());
    }

    public boolean isProcessingTimedOut(long timeoutMinutes) {
        if (this.status != ResumeVersionStatus.PROCESSING) return false;
        if (this.startedAt == null) return false;
        return this.startedAt.plus(Duration.ofMinutes(timeoutMinutes)).isBefore(Instant.now());
    }

    public boolean isQueuedTimedOut(long timeoutMinutes) {
        if (this.status != ResumeVersionStatus.QUEUED) return false;
        if (this.createdAt == null) return false;
        return this.createdAt.plus(Duration.ofMinutes(timeoutMinutes)).isBefore(Instant.now());
    }

    private static String normalizeSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return "{}";
        }
        return snapshot;
    }
}
