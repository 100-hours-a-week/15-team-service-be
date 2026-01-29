package com.sipomeokjo.commitme.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@JsonPropertyOrder({"code", "message", "data"})
public record APIResponse<T>(String code, String message, T data) {

    public static <T> ResponseEntity<APIResponse<T>> onSuccess(ResponseCode responseCode, T data) {
        return toResponseEntity(responseCode, data);
    }

    public static <T> ResponseEntity<APIResponse<T>> onSuccess(ResponseCode responseCode) {
        return toResponseEntity(responseCode, null);
    }

    public static <T> ResponseEntity<APIResponse<T>> onFailure(ResponseCode responseCode, T data) {
        return toResponseEntity(responseCode, data);
    }

    public static <T> ResponseEntity<APIResponse<T>> onFailure(ResponseCode responseCode) {
        return toResponseEntity(responseCode, null);
    }

    public static <T> APIResponse<T> body(ResponseCode responseCode, T data) {
        return new APIResponse<>(responseCode.getCode(), responseCode.getMessage(), data);
    }

    public static <T> APIResponse<T> body(ResponseCode responseCode) {
        return body(responseCode, null);
    }

    private static <T> ResponseEntity<APIResponse<T>> toResponseEntity(
            ResponseCode responseCode, T data) {
        HttpStatus status = responseCode.getHttpStatus();
        if (status == HttpStatus.NO_CONTENT) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(status).body(body(responseCode, data));
    }
}
