package com.sipomeokjo.commitme.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ResponseCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UPLOAD_PURPOSE_INVALID(HttpStatus.BAD_REQUEST, "UPLOAD_PURPOSE_INVALID", "업로드 목적이 올바르지 않습니다."),
    UPLOAD_CONTENT_TYPE_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST, "UPLOAD_CONTENT_TYPE_NOT_ALLOWED", "허용되지 않은 Content-Type입니다."),
    UPLOAD_EXTENSION_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST, "UPLOAD_EXTENSION_NOT_ALLOWED", "허용되지 않은 파일 확장자입니다."),
    UPLOAD_FILE_SIZE_EXCEEDED(
            HttpStatus.BAD_REQUEST, "UPLOAD_FILE_SIZE_EXCEEDED", "파일 크기 제한을 초과했습니다."),
    UPLOAD_FILE_SIZE_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "UPLOAD_FILE_SIZE_MISMATCH",
            "요청한 파일 크기와 업로드된 파일 크기가 일치하지 않습니다."),
    UPLOAD_ETAG_MISMATCH(HttpStatus.BAD_REQUEST, "UPLOAD_ETAG_MISMATCH", "ETag가 일치하지 않습니다."),
    USER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "USER_NAME_REQUIRED", "유저 이름은 필수 입력 값입니다."),
    USER_NAME_LENGTH_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "USER_NAME_LENGTH_OUT_OF_RANGE",
            "유저 이름은 최소 2자 이상, 최대 10자 이하여야 합니다."),
    USER_NAME_INVALID_INPUT(HttpStatus.BAD_REQUEST, "USER_NAME_INVALID_INPUT", "유저 이름이 올바르지 않습니다."),
    USER_POSITION_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_POSITION_REQUIRED", "유저 희망 포지션은 필수 입력 값입니다."),
    USER_POSITION_INVALID(HttpStatus.BAD_REQUEST, "USER_POSITION_INVALID", "유저 희망 포지션이 올바르지 않습니다."),
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "POSITION_NOT_FOUND", "포지션을 찾을 수 없습니다."),
    USER_PHONE_LENGTH_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "USER_PHONE_LENGTH_OUT_OF_RANGE",
            "전화번호는 최소 11자 이상, 최대 20자 이하여야 합니다."),
    USER_PHONE_INVALID(HttpStatus.BAD_REQUEST, "USER_PHONE_INVALID", "전화번호 형식이 올바르지 않습니다."),
    USER_POLICY_AGREED_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_POLICY_AGREED_REQUIRED", "이용약관 및 개인정보처리방침은 필수 입력 값입니다."),
    USER_POLICY_AGREED_MUST_BE_TRUE(
            HttpStatus.BAD_REQUEST,
            "USER_POLICY_AGREED_MUST_BE_TRUE",
            "이용약관 및 개인정보처리방침은 TRUE이어야 합니다."),
    USER_POLICY_AGREED_INVALID(
            HttpStatus.BAD_REQUEST, "USER_POLICY_AGREED_INVALID", "이용약관 및 개인정보처리방침이 올바르지 않습니다."),
    USER_PHONE_PRIVACY_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_PHONE_PRIVACY_REQUIRED", "전화번호 개인정보 처리방침 동의는 필수입니다."),
    INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "INVALID_CURSOR_VALUE", "커서 값이 유효하지 않거나 손상되었습니다."),
    INVALID_SIZE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_SIZE_VALUE", "사이즈 값이 유효하지 않거나 손상되었습니다."),
    INVALID_RESUME_NAME(HttpStatus.BAD_REQUEST, "RESUME_NAME_400", "이력서 이름이 올바르지 않습니다."),
    POSITION_SELECTION_REQUIRED(
            HttpStatus.BAD_REQUEST, "POSITION_SELECTION_REQUIRED", "포지션 선택은 필수 입력 값입니다."),
    CHAT_MESSAGE_INVALID(HttpStatus.BAD_REQUEST, "CHAT_MESSAGE_INVALID", "메시지 또는 첨부파일이 필요합니다."),
    USER_ALREADY_ONBOARDED(HttpStatus.CONFLICT, "USER_ALREADY_ONBOARDED", "이미 가입이 완료된 회원입니다."),
    UPLOAD_STATUS_INVALID(HttpStatus.CONFLICT, "UPLOAD_STATUS_INVALID", "업로드 상태가 올바르지 않습니다."),
    UPLOAD_EXPIRED(HttpStatus.CONFLICT, "UPLOAD_EXPIRED", "업로드 URL이 만료되었습니다."),
    RESUME_VERSION_NOT_READY(HttpStatus.CONFLICT, "RESUME_VERSION_409", "이력서 버전이 아직 준비되지 않았습니다."),
    RESUME_GENERATION_IN_PROGRESS(
            HttpStatus.CONFLICT, "RESUME_GENERATION_IN_PROGRESS", "이미 생성 중인 이력서가 있습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    USER_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_SETTINGS_NOT_FOUND", "유저 설정을 찾을 수 없습니다."),
    UPLOAD_NOT_FOUND(HttpStatus.NOT_FOUND, "UPLOAD_NOT_FOUND", "업로드 정보를 찾을 수 없습니다."),
    UPLOAD_OBJECT_NOT_FOUND(
            HttpStatus.NOT_FOUND, "UPLOAD_OBJECT_NOT_FOUND", "S3에 업로드된 객체를 찾을 수 없습니다."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_404", "이력서를 찾을 수 없습니다."),
    RESUME_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_VERSION_404", "이력서 버전을 찾을 수 없습니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "존재하지 않는 기업입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "존재하지 않는 리소스입니다."),
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OAUTH_ACCESS_DENIED", "소셜 로그인 권한이 거부되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    UPLOAD_FORBIDDEN(HttpStatus.FORBIDDEN, "UPLOAD_FORBIDDEN", "해당 업로드에 접근할 권한이 없습니다."),
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP METHOD입니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    UPLOAD_S3_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_S3_ERROR", "S3 처리 중 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "일시적으로 서비스를 사용할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
