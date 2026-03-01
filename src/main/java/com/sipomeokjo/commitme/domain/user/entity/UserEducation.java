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
@Table(name = "user_educations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEducation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EducationType type;

    @Column(nullable = false, length = 200)
    private String institution;

    @Column(name = "major_field", nullable = false, length = 200)
    private String majorField;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EducationStatus status;

    @Column(name = "start_year", nullable = false)
    private Short startYear;

    @Column(name = "start_month", nullable = false)
    private Byte startMonth;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "end_month")
    private Byte endMonth;
}
