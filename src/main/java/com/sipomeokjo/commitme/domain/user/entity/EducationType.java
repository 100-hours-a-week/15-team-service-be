package com.sipomeokjo.commitme.domain.user.entity;

import java.util.Locale;

public enum EducationType {
    PRIVATE,
    HIGH_SCHOOL,
    COLLEGE_ASSOCIATE,
    COLLEGE_BACHELOR,
    MASTER,
    PHD;

    public static EducationType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new EnumParseException("EducationType", value);
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return switch (normalized) {
                case "BACHELOR" -> COLLEGE_BACHELOR;
                case "ASSOCIATE" -> COLLEGE_ASSOCIATE;
                case "MASTER" -> MASTER;
                case "PHD", "DOCTORATE" -> PHD;
                default -> EducationType.valueOf(normalized);
            };
        } catch (IllegalArgumentException ex) {
            throw new EnumParseException("EducationType", value);
        }
    }
}
