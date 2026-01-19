package com.sipomeokjo.commitme.api.pagination;

import java.util.List;

public record PageResponse<T> (
		List<T> items,
		PageMeta pageMeta
) {
	
	public record PageMeta(
			int page,
			int size,
			long totalElements,
			int totalPages,
			boolean hasNext
	) {}
}
