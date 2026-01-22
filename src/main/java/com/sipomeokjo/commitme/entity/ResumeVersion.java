package com.sipomeokjo.commitme.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_versions",
        uniqueConstraints=@UniqueConstraint(name = "uk_resume_version", columnNames ={"resume_id", "version_no"}),
        indexes = @Index(name="idx_resume_versions_resume_id_version_no", columnList = "resume_id, version_no DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeVersion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(nullable = false)
    private String status; //v1 SUCCEEDED 고정

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable =false)
    private LocalDateTime createdAt;

    @Column(name = "committed_at")
    private LocalDateTime committedAt;

    public static ResumeVersion createV1(Long resumeId, String content) {
        ResumeVersion v = new ResumeVersion();
        v.resumeId = resumeId;
        v.versionNo = 1;
        v.status = "SUCCEEDED";
        v.content = content == null ? "" : content;
        v.createdAt = LocalDateTime.now();
        return v;
    }

    public void commitNow() {
        this.committedAt = LocalDateTime.now();
    }



}
