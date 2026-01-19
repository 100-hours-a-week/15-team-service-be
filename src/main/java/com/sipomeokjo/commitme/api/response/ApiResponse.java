package com.sipomeokjo.commitme.api.response;

public record ApiResponse<T> (
		String code,
		String message,
		T data
) {}