package com.sipomeokjo.commitme.api.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageRequestDto(
		Integer page,
		Integer size,
		String sortBy,
		String direction
) {
	public Pageable toPageable() {
		Sort sort = (sortBy == null || sortBy.isBlank())
				? Sort.unsorted()
				: Sort.by(Sort.Direction.fromString(direction == null ? "DESC" : direction), sortBy);
		
		return PageRequest.of(
				(page == null || page < 1) ? 0 : page - 1,
				(size == null || size < 1) ? 10 : Math.min(size, 50),
				sort);
	}
}
