package com.sipomeokjo.commitme.api.response;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
