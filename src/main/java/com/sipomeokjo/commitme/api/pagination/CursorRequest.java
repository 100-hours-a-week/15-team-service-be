package com.sipomeokjo.commitme.api.pagination;

public record CursorRequest(
		String after,
		Integer size
) {
	public int limit(int defaultSize) {
		return (size == null || size < 1 || size >= 50) ? defaultSize : size;
	}
}
