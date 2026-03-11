package com.sipomeokjo.commitme.domain.user.entity;

import lombok.Getter;

@Getter
public class ResumeProfileValidationException extends RuntimeException {

    private final Reason reason;

    public ResumeProfileValidationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public enum Reason {
        PHONE_COUNTRY_CODE_REQUIRED,
        PHONE_NUMBER_REQUIRED,
        PHONE_COUNTRY_CODE_INVALID,
        PHONE_NUMBER_INVALID
    }
}
