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

    public static UserEducation create(
            User user,
            EducationType type,
            String institution,
            String majorField,
            EducationStatus status,
            Short startYear,
            Byte startMonth,
            Short endYear,
            Byte endMonth) {
        UserEducation education = new UserEducation();
        education.user = user;
        education.type = type;
        education.institution = institution;
        education.majorField = majorField;
        education.status = status;
        education.startYear = startYear;
        education.startMonth = startMonth;
        education.endYear = endYear;
        education.endMonth = endMonth;
        return education;
    }

    public void update(
            EducationType type,
            String institution,
            String majorField,
            EducationStatus status,
            Short startYear,
            Byte startMonth,
            Short endYear,
            Byte endMonth) {
        this.type = type;
        this.institution = institution;
        this.majorField = majorField;
        this.status = status;
        this.startYear = startYear;
        this.startMonth = startMonth;
        this.endYear = endYear;
        this.endMonth = endMonth;
    }
}
