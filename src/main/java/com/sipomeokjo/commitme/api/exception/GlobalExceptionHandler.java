package com.sipomeokjo.commitme.api.exception;

import com.sipomeokjo.commitme.api.response.ApiResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		
		return ResponseEntity.status(errorCode.getHttpStatus()).body(
				new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null)
		);
	}
	
	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			org.springframework.validation.BindException.class,
			ConstraintViolationException.class,
			MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
		ErrorCode errorCode = ErrorCode.BAD_REQUEST;
		
		return ResponseEntity.status(errorCode.getHttpStatus()).body(
				new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null)
		);
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
		ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
		
		return ResponseEntity.status(errorCode.getHttpStatus()).body(
				new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null)
		);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		
		return ResponseEntity.status(errorCode.getHttpStatus()).body(
				new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null)
		);
	}
}
