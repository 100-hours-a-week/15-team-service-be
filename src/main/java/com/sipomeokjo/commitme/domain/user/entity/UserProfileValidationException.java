package com.sipomeokjo.commitme.domain.user.entity;

import lombok.Getter;

@Getter
public class UserProfileValidationException extends IllegalArgumentException {

    private final Reason reason;

    public UserProfileValidationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public enum Reason {
        NAME_REQUIRED,
        NAME_INVALID,
        NAME_LENGTH_OUT_OF_RANGE,
        PHONE_INVALID,
        PHONE_LENGTH_OUT_OF_RANGE
    }
}
