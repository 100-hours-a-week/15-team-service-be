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
        this.name = normalizeAndValidateName(name);
        this.phone = normalizeAndValidatePhone(phone);
        this.profileImageUrl = profileImageUrl;
        this.status = status;
    }

    public void updateProfile(
            Position position, String name, String phone, String profileImageUrl) {
        this.position = position;
        this.name = normalizeAndValidateName(name);
        this.phone = normalizeAndValidatePhone(phone);
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate(Instant deletedAt) {
        this.status = UserStatus.INACTIVE;
        this.deletedAt = deletedAt;
    }

    private static String normalizeAndValidateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.NAME_REQUIRED);
        }

        if (containsWhitespace(rawName) || containsEmoji(rawName)) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.NAME_INVALID);
        }

        String trimmed = rawName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 10) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.NAME_LENGTH_OUT_OF_RANGE);
        }

        return trimmed;
    }

    private static String normalizeAndValidatePhone(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        if (rawPhone.isBlank()) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.PHONE_LENGTH_OUT_OF_RANGE);
        }

        if (rawPhone.length() < 11 || rawPhone.length() > 20) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.PHONE_LENGTH_OUT_OF_RANGE);
        }

        if (!rawPhone.matches("^[0-9]+$")) {
            throw new UserProfileValidationException(
                    UserProfileValidationException.Reason.PHONE_INVALID);
        }

        return rawPhone;
    }

    private static boolean containsWhitespace(String value) {
        return value.codePoints().anyMatch(Character::isWhitespace);
    }

    private static boolean containsEmoji(String value) {
        return value.codePoints().anyMatch(User::isEmojiCodePoint);
    }

    private static boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F)
                || (codePoint >= 0x1F300 && codePoint <= 0x1F5FF)
                || (codePoint >= 0x1F680 && codePoint <= 0x1F6FF)
                || (codePoint >= 0x1F900 && codePoint <= 0x1F9FF)
                || (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x26FF)
                || (codePoint >= 0x2700 && codePoint <= 0x27BF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF)
                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F);
    }
}
