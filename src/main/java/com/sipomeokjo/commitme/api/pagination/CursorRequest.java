package com.sipomeokjo.commitme.api.pagination;

import java.time.Instant;

public record CursorRequest(
		Instant cursorCreatedAt,
		Long cursor,
		Integer size
) {
	public int limit(int defaultSize) {
		return (size == null || size < 1 || size >= 50) ? defaultSize : size;
	}
}
