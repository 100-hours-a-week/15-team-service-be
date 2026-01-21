package com.sipomeokjo.commitme.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resumes_user_id_id", columnList = "user_id, id DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 18)
    private String name;

    private String position;
    private String company;

    @Column(name = "current_version_no", nullable = false)
    private int currentVersionNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Resume create(Long userId, String name, String position, String company){
        Resume r = new Resume();
        r.userId = userId;
        r.name = name;
        r.position = position;
        r.company = company;
        r.currentVersionNo = 1;
        r.createdAt = LocalDateTime.now();
        r.updatedAt = r.createdAt;
        return r;
    }

    public void rename(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCurrentVersionNo(int versionNo) {
        this.currentVersionNo = versionNo;
        this.updatedAt = LocalDateTime.now();

    }

}
