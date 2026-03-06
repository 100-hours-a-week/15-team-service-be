package com.sipomeokjo.commitme.domain.user.entity;

import java.util.Locale;

public enum EducationStatus {
    GRADUATED,
    DEFERRED,
    ENROLLED,
    DROPPED,
    COMPLETED;

    public static EducationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new EnumParseException("EducationStatus", value);
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return EducationStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new EnumParseException("EducationStatus", value);
        }
    }
}
