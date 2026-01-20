package com.sipomeokjo.commitme.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.ResponseEntity;

@JsonPropertyOrder({ "code", "message", "data" })
public record ApiResponse<T>(
        String code,
        String message,
        T data
) {

    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(SuccessCode successCode, T data) {
        ApiResponse<T> body = new ApiResponse<>(successCode.getCode(), successCode.getMessage(), data);
        return new ResponseEntity<>(body, successCode.getHttpStatus());
    }

    public static <T> ResponseEntity<ApiResponse<T>> onSuccess(SuccessCode successCode) {
        ApiResponse<T> body = new ApiResponse<>(successCode.getCode(), successCode.getMessage(), null);
        return new ResponseEntity<>(body, successCode.getHttpStatus());
    }

    public static ResponseEntity<Void> noContent(SuccessCode successCode) {
        return new ResponseEntity<>(successCode.getHttpStatus());
    }

    public static <T> ResponseEntity<ApiResponse<T>> onFailure(ErrorCode errorCode, T data) {
        ApiResponse<T> body = new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
        return new ResponseEntity<>(body, errorCode.getHttpStatus());
    }

    public static <T> ResponseEntity<ApiResponse<T>> onFailure(ErrorCode errorCode) {
        ApiResponse<T> body = new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
        return new ResponseEntity<>(body, errorCode.getHttpStatus());
    }
}