package com.sipomeokjo.commitme.api.pagination;

import java.time.Instant;

public record CursorRequestDto(
		Instant cursorCreatedAt,
		Long cursor,
		Integer size
) {
	public int limit() {
		return (size == null || size < 1) ? 10 : Math.min(size, 50);
	}
}
