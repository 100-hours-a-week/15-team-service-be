package com.sipomeokjo.commitme.domain.resume.entity;

import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "resume")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "current_version_no")
    private Integer currentVersionNo;

    @Column(name = "active_version_no")
    private Integer activeVersionNo;

    public static Resume create(User user, Position position, Company company, String name) {
        Resume r = new Resume();
        r.user = user;
        r.position = position;
        r.company = company;
        r.name = name;
        r.currentVersionNo = 1; // 최초 버전 1로 시작
        return r;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setCurrentVersionNo(int versionNo) {
        this.currentVersionNo = versionNo;
    }
}
