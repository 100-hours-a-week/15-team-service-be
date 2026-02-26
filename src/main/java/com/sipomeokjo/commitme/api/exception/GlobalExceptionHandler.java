package com.sipomeokjo.commitme.api.exception;

import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.api.sse.SseExceptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(
                "[EXCEPTION] business uri={} method={} accept={} code={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                errorCode.getCode());
        return toResponse(errorCode, request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        org.springframework.validation.BindException.class,
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<?> handleBadRequest(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        log.warn(
                "[EXCEPTION] bad_request uri={} method={} accept={} type={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                e.getClass().getSimpleName());
        return toResponse(errorCode, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        log.warn(
                "[EXCEPTION] method_not_allowed uri={} method={} accept={} supported={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                e.getSupportedHttpMethods());
        return toResponse(errorCode, request);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<?> handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException e, HttpServletRequest request) {
        if (isSseRequest(request) && SseExceptionUtils.isClientDisconnected(e)) {
            return ResponseEntity.noContent().build();
        }

        log.error(
                "[EXCEPTION] internal_error uri={} method={} accept={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException e, HttpServletRequest request) {
        if (isSseRequest(request)) {
            log.debug("[EXCEPTION] sse_timeout_trace", e);
            return ResponseEntity.noContent().build();
        }

        log.error(
                "[EXCEPTION] internal_error uri={} method={} accept={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e, HttpServletRequest request) {
        if (isSseRequest(request) && SseExceptionUtils.isClientDisconnected(e)) {
            return ResponseEntity.noContent().build();
        }

        log.error(
                "[EXCEPTION] internal_error uri={} method={} accept={}",
                safeUri(request),
                safeMethod(request),
                safeAccept(request),
                e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<?> toResponse(ErrorCode errorCode, HttpServletRequest request) {
        if (isSseRequest(request)) {
            return ResponseEntity.status(errorCode.getHttpStatus()).build();
        }
        return APIResponse.onFailure(errorCode);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private String safeUri(HttpServletRequest request) {
        return request == null ? "" : request.getRequestURI();
    }

    private String safeMethod(HttpServletRequest request) {
        return request == null ? "" : request.getMethod();
    }

    private String safeAccept(HttpServletRequest request) {
        return request == null ? "" : request.getHeader("Accept");
    }
}
