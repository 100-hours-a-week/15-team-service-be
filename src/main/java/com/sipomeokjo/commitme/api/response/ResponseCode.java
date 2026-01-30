package com.sipomeokjo.commitme.api.response;

import org.springframework.http.HttpStatus;

public sealed interface ResponseCode permits SuccessCode, ErrorCode {
    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
