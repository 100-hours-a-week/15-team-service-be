package com.sipomeokjo.commitme.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ResponseCode {
	BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
	NAME_INVALID_INPUT(HttpStatus.BAD_REQUEST, "NAME_INVALID_INPUT", "이름은 공백을 제외하고 2자 이상 입력해야 합니다."),
	POSITION_SELECTION_REQUIRED(HttpStatus.BAD_REQUEST, "POSITION_SELECTION_REQUIRED", "하나의 포지션을 선택해야 합니다."),
	USER_PRIVACY_REQUIRED(HttpStatus.BAD_REQUEST, "USER_PRIVACY_REQUIRED", "개인정보 처리방침 동의는 필수입니다."),
	USER_PHONE_PRIVACY_REQUIRED(HttpStatus.BAD_REQUEST, "USER_PHONE_PRIVACY_REQUIRED", "전화번호 개인정보 처리방침 동의는 필수입니다."),
	USER_ALREADY_ONBOARDED(HttpStatus.CONFLICT, "USER_ALREADY_ONBOARDED", "이미 가입이 완료된 회원입니다."),
	OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OAUTH_ACCESS_DENIED", "소셜 로그인 권한이 거부되었습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP METHOD입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "일시적으로 서비스를 사용할 수 없습니다."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_404", "이력서를 찾을 수 없습니다."),
    RESUME_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_VERSION_404", "이력서 버전을 찾을 수 없습니다."),
    INVALID_RESUME_NAME(HttpStatus.BAD_REQUEST, "RESUME_NAME_400", "이력서 이름이 올바르지 않습니다."),
    RESUME_VERSION_NOT_READY(HttpStatus.CONFLICT, "RESUME_VERSION_409", "이력서 버전이 아직 준비되지 않았습니다.");

    private final HttpStatus httpStatus;
	private final String code;
	private final String message;
	
	ErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
