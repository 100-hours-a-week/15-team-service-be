package com.sipomeokjo.commitme.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode implements ResponseCode {
    OK(HttpStatus.OK, "SUCCESS", "요청이 성공적으로 처리되었습니다."),
    LOGIN_URL_ISSUED(HttpStatus.OK, "SUCCESS", "로그인 URL 발급에 성공했습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "SUCCESS", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "SUCCESS", "로그아웃에 성공했습니다."),
    POSITION_LIST_FETCHED(HttpStatus.OK, "SUCCESS", "포지션 목록 조회에 성공했습니다."),
    USER_PROFILE_FETCHED(HttpStatus.OK, "SUCCESS", "내 정보 조회에 성공했습니다."),
    USER_PROFILE_UPDATED(HttpStatus.OK, "SUCCESS", "회원 정보가 수정되었습니다."),
    ONBOARDING_COMPLETED(HttpStatus.OK, "SUCCESS", "회원가입이 완료되었습니다."),
    USER_SETTINGS_FETCHED(HttpStatus.OK, "SUCCESS", "사용자 설정 조회에 성공했습니다."),
    USER_SETTINGS_UPDATED(HttpStatus.OK, "SUCCESS", "사용자 설정 수정에 성공했습니다."),
    CHATROOM_FETCHED(HttpStatus.OK, "SUCCESS", "채팅방 목록 조회에 성공했습니다."),
    CHAT_HISTORY_FETCHED(HttpStatus.OK, "SUCCESS", "채팅 내역 조회에 성공했습니다."),
    UPLOAD_URL_ISSUED(HttpStatus.CREATED, "CREATED", "업로드 URL 발급에 성공했습니다."),
    UPLOAD_CONFIRMED(HttpStatus.OK, "SUCCESS", "업로드 완료 확정에 성공했습니다."),
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
