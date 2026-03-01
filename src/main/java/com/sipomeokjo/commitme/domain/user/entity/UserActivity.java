package com.sipomeokjo.commitme.domain.user.entity;

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
@Table(name = "user_activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_name", nullable = false, length = 200)
    private String activityName;

    @Column(nullable = false, length = 200)
    private String organization;

    @Column(name = "activity_year", nullable = false)
    private Short activityYear;

    @Column(columnDefinition = "text")
    private String description;
}
