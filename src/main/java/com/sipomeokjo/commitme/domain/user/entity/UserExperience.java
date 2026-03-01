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
@Table(name = "user_experiences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "position_title", nullable = false, length = 120)
    private String positionTitle;

    @Column(name = "department_name", length = 120)
    private String departmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(name = "start_year", nullable = false)
    private Short startYear;

    @Column(name = "start_month", nullable = false)
    private Byte startMonth;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "end_month")
    private Byte endMonth;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;

    @Column(columnDefinition = "text")
    private String description;
}
