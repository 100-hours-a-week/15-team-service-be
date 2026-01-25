package com.sipomeokjo.commitme.domain.userSetting.entity;

import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSetting extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "interview_resume_defaults_enabled", nullable = false)
    private boolean interviewResumeDefaultsEnabled;

    @Builder
    public UserSetting(Long id, User user, boolean notificationEnabled, boolean interviewResumeDefaultsEnabled) {
        this.id = id;
        this.user = user;
        this.notificationEnabled = notificationEnabled;
        this.interviewResumeDefaultsEnabled = interviewResumeDefaultsEnabled;
    }

    public static UserSetting defaultSetting(User user) {
        return UserSetting.builder()
                .user(user)
                .notificationEnabled(true)
                .interviewResumeDefaultsEnabled(false)
                .build();
    }

    public void update(Boolean notificationEnabled, Boolean interviewResumeDefaultsEnabled) {
        if (notificationEnabled != null) {
            this.notificationEnabled = notificationEnabled;
        }
        if (interviewResumeDefaultsEnabled != null) {
            this.interviewResumeDefaultsEnabled = interviewResumeDefaultsEnabled;
        }
    }
}
