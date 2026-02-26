package com.sipomeokjo.commitme.domain.user.entity;

import com.sipomeokjo.commitme.domain.position.entity.Position;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Column(length = 10)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 1024)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public User(
            Long id,
            Position position,
            String name,
            String phone,
            String profileImageUrl,
            UserStatus status,
            Instant deletedAt) {
        this.id = id;
        this.position = position;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
        this.deletedAt = deletedAt;
    }

    public void updateOnboarding(
            Position position,
            String name,
            String phone,
            String profileImageUrl,
            UserStatus status) {
        this.position = position;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
    }

    public void updateProfile(
            Position position, String name, String phone, String profileImageUrl) {
        this.position = position;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate(Instant deletedAt) {
        this.status = UserStatus.INACTIVE;
        this.deletedAt = deletedAt;
    }

    public void restoreForRejoin() {
        this.status = UserStatus.PENDING;
        this.deletedAt = null;
    }
}
