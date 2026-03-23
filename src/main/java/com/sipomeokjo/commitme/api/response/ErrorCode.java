package com.sipomeokjo.commitme.api.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ResponseCode {

    // 유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    USER_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_SETTINGS_NOT_FOUND", "유저 설정을 찾을 수 없습니다."),
    USER_ALREADY_ONBOARDED(HttpStatus.CONFLICT, "USER_ALREADY_ONBOARDED", "이미 가입이 완료된 회원입니다."),
    USER_NAME_INVALID(HttpStatus.BAD_REQUEST, "USER_NAME_INVALID_INPUT", "유저 이름이 올바르지 않습니다."),
    USER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "USER_NAME_REQUIRED", "유저 이름은 필수 입력 값입니다."),
    USER_NAME_LENGTH_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "USER_NAME_LENGTH_OUT_OF_RANGE",
            "유저 이름은 최소 2자 이상, 최대 10자 이하여야 합니다."),
    USER_POSITION_INVALID(HttpStatus.BAD_REQUEST, "USER_POSITION_INVALID", "유저 희망 포지션이 올바르지 않습니다."),
    USER_POSITION_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_POSITION_REQUIRED", "유저 희망 포지션은 필수 입력 값입니다."),
    USER_PHONE_INVALID(HttpStatus.BAD_REQUEST, "USER_PHONE_INVALID", "전화번호 형식이 올바르지 않습니다."),
    USER_PHONE_LENGTH_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "USER_PHONE_LENGTH_OUT_OF_RANGE",
            "전화번호는 최소 11자 이상, 최대 20자 이하여야 합니다."),
    USER_POLICY_AGREED_INVALID(
            HttpStatus.BAD_REQUEST, "USER_POLICY_AGREED_INVALID", "이용약관 및 개인정보처리방침이 올바르지 않습니다."),
    USER_POLICY_AGREED_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_POLICY_AGREED_REQUIRED", "이용약관 및 개인정보처리방침은 필수 입력 값입니다."),
    USER_POLICY_AGREED_MUST_BE_TRUE(
            HttpStatus.BAD_REQUEST,
            "USER_POLICY_AGREED_MUST_BE_TRUE",
            "이용약관 및 개인정보처리방침은 TRUE이어야 합니다."),
    USER_PHONE_PRIVACY_REQUIRED(
            HttpStatus.BAD_REQUEST, "USER_PHONE_PRIVACY_REQUIRED", "전화번호 개인정보 처리방침 동의는 필수입니다."),

    // 포지션
    POSITION_NOT_FOUND(HttpStatus.NOT_FOUND, "POSITION_NOT_FOUND", "포지션을 찾을 수 없습니다."),

    // 이력서
    POSITION_SELECTION_REQUIRED(
            HttpStatus.BAD_REQUEST, "POSITION_SELECTION_REQUIRED", "포지션 선택은 필수 입력 값입니다."),
    RESUME_GENERATION_IN_PROGRESS(
            HttpStatus.CONFLICT, "RESUME_GENERATION_IN_PROGRESS", "이미 생성 중인 이력서가 있습니다."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_404", "이력서를 찾을 수 없습니다."),
    RESUME_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_VERSION_404", "이력서 버전을 찾을 수 없습니다."),
    RESUME_VERSION_NOT_READY(HttpStatus.CONFLICT, "RESUME_VERSION_409", "이력서 버전이 아직 준비되지 않았습니다."),
    INVALID_RESUME_NAME(HttpStatus.BAD_REQUEST, "RESUME_NAME_400", "이력서 이름이 올바르지 않습니다."),
    RESUME_EDIT_IN_PROGRESS(HttpStatus.CONFLICT, "RESUME_EDIT_IN_PROGRESS", "이력서 수정 작업이 진행 중입니다."),

    // 이력서 개인정보
    RESUME_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_PROFILE_404", "이력서 프로필을 찾을 수 없습니다."),
    RESUME_PROFILE_DUPLICATE_NAME(
            HttpStatus.CONFLICT, "RESUME_PROFILE_DUPLICATE_NAME", "이미 사용 중인 이력서 이름입니다."),
    INVALID_RESUME_PROFILE_INPUT(
            HttpStatus.BAD_REQUEST, "INVALID_RESUME_PROFILE_INPUT", "이력서 프로필 입력이 올바르지 않습니다."),
    RESUME_PROFILE_NAME_REQUIRED(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_NAME_REQUIRED", "이름은 필수 입력 값입니다."),
    RESUME_PROFILE_NAME_LENGTH_OUT_OF_RANGE(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_NAME_LENGTH_OUT_OF_RANGE",
            "이름은 최대 10자 이하여야 합니다."),
    RESUME_PROFILE_PHONE_COUNTRY_CODE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_PHONE_COUNTRY_CODE_REQUIRED",
            "국가번호와 전화번호는 함께 입력해야 합니다."),
    RESUME_PROFILE_PHONE_NUMBER_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_PHONE_NUMBER_REQUIRED",
            "국가번호와 전화번호는 함께 입력해야 합니다."),
    RESUME_PROFILE_PHONE_COUNTRY_CODE_INVALID(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_PHONE_COUNTRY_CODE_INVALID",
            "국가번호 형식이 올바르지 않습니다."),
    RESUME_PROFILE_PHONE_NUMBER_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_PHONE_NUMBER_INVALID", "전화번호 형식이 올바르지 않습니다."),
    RESUME_PROFILE_TECH_STACK_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_TECH_STACK_LIMIT_EXCEEDED",
            "기술 스택은 최대 10개까지 입력할 수 있습니다."),
    RESUME_PROFILE_EXPERIENCE_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_EXPERIENCE_LIMIT_EXCEEDED",
            "경력은 최대 5개까지 입력할 수 있습니다."),
    RESUME_PROFILE_EDUCATION_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_EDUCATION_LIMIT_EXCEEDED",
            "학력은 최대 5개까지 입력할 수 있습니다."),
    RESUME_PROFILE_ACTIVITY_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_ACTIVITY_LIMIT_EXCEEDED",
            "대외활동은 최대 10개까지 입력할 수 있습니다."),
    RESUME_PROFILE_CERTIFICATE_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_CERTIFICATE_LIMIT_EXCEEDED",
            "자격증은 최대 10개까지 입력할 수 있습니다."),
    RESUME_PROFILE_ITEM_ID_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_ITEM_ID_INVALID", "수정 대상 항목 ID가 올바르지 않습니다."),
    RESUME_PROFILE_DATE_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_DATE_INVALID", "날짜 형식이 올바르지 않습니다."),
    RESUME_PROFILE_EXPERIENCE_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_EXPERIENCE_INVALID", "경력 입력값이 올바르지 않습니다."),
    RESUME_PROFILE_EDUCATION_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_EDUCATION_INVALID", "학력 입력값이 올바르지 않습니다."),
    RESUME_PROFILE_ACTIVITY_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_ACTIVITY_INVALID", "대외활동 입력값이 올바르지 않습니다."),
    RESUME_PROFILE_CERTIFICATE_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_CERTIFICATE_INVALID", "자격증 입력값이 올바르지 않습니다."),
    RESUME_PROFILE_EMPLOYMENT_TYPE_INVALID(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_EMPLOYMENT_TYPE_INVALID",
            "고용 형태 값이 올바르지 않습니다."),
    RESUME_PROFILE_EDUCATION_TYPE_INVALID(
            HttpStatus.BAD_REQUEST, "RESUME_PROFILE_EDUCATION_TYPE_INVALID", "학력 구분 값이 올바르지 않습니다."),
    RESUME_PROFILE_EDUCATION_STATUS_INVALID(
            HttpStatus.BAD_REQUEST,
            "RESUME_PROFILE_EDUCATION_STATUS_INVALID",
            "학력 상태 값이 올바르지 않습니다."),

    // 채팅
    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_INVALID(HttpStatus.BAD_REQUEST, "CHAT_MESSAGE_INVALID", "메시지 또는 첨부파일이 필요합니다."),

    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_404", "알림을 찾을 수 없습니다."),

    // 회사
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", "존재하지 않는 기업입니다."),
    COMPANY_DUPLICATED(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다."),

    // 파일 업로드 및 조회
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

    // S3
    UPLOAD_NOT_FOUND(HttpStatus.NOT_FOUND, "UPLOAD_NOT_FOUND", "업로드 정보를 찾을 수 없습니다."),
    UPLOAD_OBJECT_NOT_FOUND(
            HttpStatus.NOT_FOUND, "UPLOAD_OBJECT_NOT_FOUND", "S3에 업로드된 객체를 찾을 수 없습니다."),
    UPLOAD_STATUS_INVALID(HttpStatus.CONFLICT, "UPLOAD_STATUS_INVALID", "업로드 상태가 올바르지 않습니다."),
    UPLOAD_EXPIRED(HttpStatus.CONFLICT, "UPLOAD_EXPIRED", "업로드 URL이 만료되었습니다."),
    UPLOAD_FORBIDDEN(HttpStatus.FORBIDDEN, "UPLOAD_FORBIDDEN", "해당 업로드에 접근할 권한이 없습니다."),
    UPLOAD_S3_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_S3_ERROR", "S3 처리 중 오류가 발생했습니다."),

    // 권한
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
    CSRF_INVALID(HttpStatus.FORBIDDEN, "AUTH_CSRF_INVALID", "보안 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않습니다."),

    // OAUTH
    OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OAUTH_ACCESS_DENIED", "소셜 로그인 권한이 거부되었습니다."),
    OAUTH_ACCOUNT_WITHDRAWN(
            HttpStatus.FORBIDDEN, "OAUTH_ACCOUNT_WITHDRAWN", "탈퇴한 계정입니다. 1일 후 재가입 가능합니다."),

    // 페이지네이션
    INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "INVALID_CURSOR_VALUE", "커서 값이 유효하지 않거나 손상되었습니다."),
    INVALID_SIZE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_SIZE_VALUE", "사이즈 값이 유효하지 않거나 손상되었습니다."),
    INVALID_SORT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_SORT_VALUE", "정렬 값이 유효하지 않거나 손상되었습니다."),
    INVALID_KEYWORD_VALUE(
            HttpStatus.BAD_REQUEST, "INVALID_KEYWORD_VALUE", "검색어 값이 유효하지 않거나 손상되었습니다."),

    // 면접
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_NOT_FOUND", "면접을 찾을 수 없습니다."),
    INTERVIEW_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "INTERVIEW_ALREADY_ENDED", "이미 종료된 면접입니다."),
    INTERVIEW_SESSION_INVALID(
            HttpStatus.BAD_REQUEST, "INTERVIEW_SESSION_INVALID", "유효하지 않은 면접 세션입니다."),

    // AI
    AI_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", "AI 서버에 연결할 수 없습니다."),
    AI_SERVICE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI_SERVICE_TIMEOUT", "AI 서버 응답 시간이 초과되었습니다."),
    AI_CIRCUIT_BREAKER_OPEN(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AI_CIRCUIT_BREAKER_OPEN",
            "AI 서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."),

    // 전역
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    METHOD_NOT_ALLOWED(
            HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP METHOD입니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
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
