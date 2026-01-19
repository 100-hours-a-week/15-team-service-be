package com.sipomeokjo.commitme.api.exception;

import com.sipomeokjo.commitme.api.response.ErrorCode;

public class BusinessException extends RuntimeException {
	
	private final ErrorCode errorCode;
	
	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
	
	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
