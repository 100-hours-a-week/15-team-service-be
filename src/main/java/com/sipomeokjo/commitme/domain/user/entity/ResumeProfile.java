package com.sipomeokjo.commitme.domain.user.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "resume_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeProfile extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "phone_country_code", length = 8)
    private String phoneCountryCode;

    @Column(name = "phone_national_number", length = 30)
    private String phoneNationalNumber;

    @Column(columnDefinition = "text")
    private String summary;

    public static ResumeProfile create(
            User user, String phoneCountryCode, String phoneNationalNumber, String summary) {
        ResumeProfile profile = new ResumeProfile();
        profile.user = user;
        profile.update(phoneCountryCode, phoneNationalNumber, summary);
        return profile;
    }

    public void update(String phoneCountryCode, String phoneNationalNumber, String summary) {
        String normalizedCountryCode = normalizeNullable(phoneCountryCode);
        String normalizedPhoneNumber = normalizeNullable(phoneNationalNumber);

        validatePhone(normalizedCountryCode, normalizedPhoneNumber);

        this.phoneCountryCode = normalizedCountryCode;
        this.phoneNationalNumber = normalizedPhoneNumber;
        this.summary = normalizeNullable(summary);
    }

    private void validatePhone(String countryCode, String number) {
        if (countryCode == null && number == null) {
            return;
        }

        if (countryCode == null) {
            throw new ResumeProfileValidationException(
                    ResumeProfileValidationException.Reason.PHONE_COUNTRY_CODE_REQUIRED);
        }
        if (number == null) {
            throw new ResumeProfileValidationException(
                    ResumeProfileValidationException.Reason.PHONE_NUMBER_REQUIRED);
        }
        if (!countryCode.matches("\\+\\d{1,3}")) {
            throw new ResumeProfileValidationException(
                    ResumeProfileValidationException.Reason.PHONE_COUNTRY_CODE_INVALID);
        }
        if (!number.matches("\\d{4,12}")) {
            throw new ResumeProfileValidationException(
                    ResumeProfileValidationException.Reason.PHONE_NUMBER_INVALID);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
