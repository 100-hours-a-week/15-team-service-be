package com.sipomeokjo.commitme.api.validation;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;

public final class KeywordValidator {

    private KeywordValidator() {}

    public static String normalize(String keyword, int maxLength) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty() || trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.INVALID_KEYWORD_VALUE);
        }
        return trimmed;
    }
}
