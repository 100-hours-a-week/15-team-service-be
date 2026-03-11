package com.sipomeokjo.commitme.domain.user.mapper;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.user.entity.ResumeProfileValidationException;
import com.sipomeokjo.commitme.domain.user.entity.UserProfileValidationException;
import org.springframework.stereotype.Component;

@Component
public class UserValidationExceptionMapper {

    public BusinessException toBusinessException(UserProfileValidationException ex) {
        return switch (ex.getReason()) {
            case NAME_REQUIRED -> new BusinessException(ErrorCode.USER_NAME_REQUIRED);
            case NAME_INVALID -> new BusinessException(ErrorCode.USER_NAME_INVALID);
            case NAME_LENGTH_OUT_OF_RANGE ->
                    new BusinessException(ErrorCode.USER_NAME_LENGTH_OUT_OF_RANGE);
            case PHONE_INVALID -> new BusinessException(ErrorCode.USER_PHONE_INVALID);
            case PHONE_LENGTH_OUT_OF_RANGE ->
                    new BusinessException(ErrorCode.USER_PHONE_LENGTH_OUT_OF_RANGE);
        };
    }

    public BusinessException toBusinessException(ResumeProfileValidationException ex) {
        return switch (ex.getReason()) {
            case PHONE_COUNTRY_CODE_REQUIRED ->
                    new BusinessException(ErrorCode.RESUME_PROFILE_PHONE_COUNTRY_CODE_REQUIRED);
            case PHONE_NUMBER_REQUIRED ->
                    new BusinessException(ErrorCode.RESUME_PROFILE_PHONE_NUMBER_REQUIRED);
            case PHONE_COUNTRY_CODE_INVALID ->
                    new BusinessException(ErrorCode.RESUME_PROFILE_PHONE_COUNTRY_CODE_INVALID);
            case PHONE_NUMBER_INVALID ->
                    new BusinessException(ErrorCode.RESUME_PROFILE_PHONE_NUMBER_INVALID);
        };
    }
}
