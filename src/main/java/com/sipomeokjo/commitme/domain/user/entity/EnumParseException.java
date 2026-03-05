package com.sipomeokjo.commitme.domain.user.entity;

public class EnumParseException extends IllegalArgumentException {

    public EnumParseException(String enumType, String rawValue) {
        super(enumType + ":" + rawValue);
    }
}
