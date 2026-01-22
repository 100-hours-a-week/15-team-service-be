package com.sipomeokjo.commitme.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode {
    OK(HttpStatus.OK, "SUCCESS", "요청이 성공적으로 처리되었습니다."),
    LOGIN_URL_ISSUED(HttpStatus.OK, "SUCCESS", "로그인 URL 발급에 성공했습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "SUCCESS", "로그인에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "CREATED", "생성에 성공했습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "NO_CONTENT", "성공적으로 처리되었으며, 반환할 데이터가 없습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
