package com.sipomeokjo.commitme.domain.resume.dto;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;

public enum ResumeSortBy {
    UPDATED_DESC,
    UPDATED_ASC;

    public static ResumeSortBy from(String value) {
        if (value == null || value.isBlank()) {
            return UPDATED_DESC;
        }

        try {
            return ResumeSortBy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_SORT_VALUE);
        }
    }
}
