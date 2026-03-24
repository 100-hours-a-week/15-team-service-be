package com.sipomeokjo.commitme.domain.resume.document;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Instant;
import java.util.List;
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
@Document(collection = "resumes")
@CompoundIndexes({
    @CompoundIndex(name = "ux_resumes_resume_id", def = "{'resume_id': 1}", unique = true),
    @CompoundIndex(
            name = "ix_resumes_user_updated_desc",
            def = "{'user_id': 1, 'updated_at': -1, 'resume_id': -1}"),
    @CompoundIndex(
            name = "ix_resumes_user_updated_asc",
            def = "{'user_id': 1, 'updated_at': 1, 'resume_id': 1}"),
    @CompoundIndex(name = "ix_resumes_user_pending", def = "{'user_id': 1, 'has_pending_work': 1}")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeDocument {

    @Id private String id;

    @Field("resume_id")
    private Long resumeId;

    @Field("user_id")
    private Long userId;

    @Field("position_id")
    private Long positionId;

    @Field("position_name")
    private String positionName;

    @Field("company_id")
    private Long companyId;

    @Field("company_name")
    private String companyName;

    @Field("name")
    private String name;

    @Field("current_version_no")
    private Integer currentVersionNo;

    @Field("latest_succeeded_version_no")
    private Integer latestSucceededVersionNo;

    @Field("latest_preview_version_no")
    private Integer latestPreviewVersionNo;

    @Field("has_unseen_preview")
    private boolean hasUnseenPreview;

    @Field("has_pending_work")
    private boolean hasPendingWork;

    @Field("profile_snapshot")
    private String profileSnapshot;

    @Field("last_applied_version_no")
    private Integer lastAppliedVersionNo;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    public static ResumeDocument create(
            Long resumeId,
            Long userId,
            Long positionId,
            String positionName,
            Long companyId,
            String companyName,
            String name,
            String profileSnapshot) {
        ResumeDocument doc = new ResumeDocument();
        doc.resumeId = resumeId;
        doc.userId = userId;
        doc.positionId = positionId;
        doc.positionName = positionName;
        doc.companyId = companyId;
        doc.companyName = companyName;
        doc.name = name;
        doc.currentVersionNo = 1;
        doc.latestSucceededVersionNo = null;
        doc.latestPreviewVersionNo = null;
        doc.hasUnseenPreview = false;
        doc.hasPendingWork = true;
        doc.profileSnapshot = profileSnapshot;
        doc.lastAppliedVersionNo = 0;
        return doc;
    }

    public static ResumeDocument rebuildFrom(
            ResumeDocument existing, List<ResumeEventDocument> eventsAsc) {
        ResumeDocument doc =
                ResumeDocument.create(
                        existing.resumeId,
                        existing.userId,
                        existing.positionId,
                        existing.positionName,
                        existing.companyId,
                        existing.companyName,
                        existing.name,
                        existing.profileSnapshot);
        doc.id = existing.id;
        doc.createdAt = existing.createdAt;
        doc.lastAppliedVersionNo = 0;
        doc.hasPendingWork = false;

        for (ResumeEventDocument event : eventsAsc) {
            int vno = event.getVersionNo();
            ResumeVersionStatus status = event.getStatus();

            if (status == ResumeVersionStatus.SUCCEEDED) {
                boolean isCreate = (vno == 1);
                doc.applyAiSuccess(vno, isCreate);
                if (event.getPreviewShownAt() != null) {
                    doc.applyPreviewShown();
                }
                if (event.getCommittedAt() != null) {
                    doc.applyVersionCommitted(vno);
                }
            } else if (status == ResumeVersionStatus.FAILED) {
                doc.applyAiFailure(vno);
            } else if (status == ResumeVersionStatus.QUEUED
                    || status == ResumeVersionStatus.PROCESSING) {
                doc.markPendingWorkStarted();
            }
        }
        return doc;
    }

    public void applyAiSuccess(int versionNo, boolean isCreate) {
        if (lastAppliedVersionNo != null && versionNo <= lastAppliedVersionNo) {
            return;
        }
        this.latestSucceededVersionNo = versionNo;
        this.latestPreviewVersionNo = versionNo;
        this.hasUnseenPreview = true;
        this.hasPendingWork = false;
        if (isCreate) {
            this.currentVersionNo = versionNo;
        }
        this.lastAppliedVersionNo = versionNo;
    }

    public void applyAiFailure(int versionNo) {
        if (lastAppliedVersionNo != null && versionNo <= lastAppliedVersionNo) {
            return;
        }
        this.hasPendingWork = false;
        this.lastAppliedVersionNo = versionNo;
    }

    public void applyVersionCommitted(int versionNo) {
        this.currentVersionNo = versionNo;
        if (this.lastAppliedVersionNo == null || versionNo > this.lastAppliedVersionNo) {
            this.lastAppliedVersionNo = versionNo;
        }
    }

    public void applyPreviewShown() {
        this.hasUnseenPreview = false;
    }

    public void markPendingWorkStarted() {
        this.hasPendingWork = true;
    }
}
