package com.sipomeokjo.commitme.api.pagination;

import java.time.Instant;
import java.util.List;

public record CursorResponse<T>(
		List<T> items,
		CursorMeta cursorMeta
) {
	
	public record CursorMeta(
			boolean hasNext,
			Instant nextCursorCreatedAt,
			Long nextCursorId
	) {}
}
