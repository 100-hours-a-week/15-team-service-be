package com.sipomeokjo.commitme.domain.user.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_certificates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCertificate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CertificateType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "grade_or_score", length = 120)
    private String gradeOrScore;

    @Column(length = 200)
    private String issuer;

    @Column(name = "acquired_year", nullable = false)
    private Short acquiredYear;

    @Column(name = "acquired_month", nullable = false)
    private Byte acquiredMonth;
}
