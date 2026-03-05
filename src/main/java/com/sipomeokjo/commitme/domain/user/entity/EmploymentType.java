package com.sipomeokjo.commitme.domain.user.entity;

import java.util.Locale;

public enum EmploymentType {
    INTERN,
    CONTRACT,
    FULL_TIME,
    BUSINESS,
    FREELANCE;

    public static EmploymentType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new EnumParseException("EmploymentType", value);
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if ("SOLE_PROPRIETOR".equals(normalized)) {
            return BUSINESS;
        }
        try {
            return EmploymentType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new EnumParseException("EmploymentType", value);
        }
    }
}
