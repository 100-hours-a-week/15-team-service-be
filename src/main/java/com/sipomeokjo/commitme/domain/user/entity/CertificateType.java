package com.sipomeokjo.commitme.domain.user.entity;

import java.util.Locale;

public enum CertificateType {
    CERTIFICATE,
    LANGUAGE;

    public static CertificateType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new EnumParseException("CertificateType", value);
        }
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CERTIFICATE" -> CERTIFICATE;
            case "LANGUAGE", "LANG" -> LANGUAGE;
            default -> throw new EnumParseException("CertificateType", value);
        };
    }
}
