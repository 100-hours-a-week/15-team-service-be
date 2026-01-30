package com.sipomeokjo.commitme.api.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PagingRequest(Integer page, Integer size, String sortBy, String direction) {
    public Pageable toPageable(int defaultSize) {
        Sort sort =
                (sortBy == null || sortBy.isBlank())
                        ? Sort.unsorted()
                        : Sort.by(
                                Sort.Direction.fromString(direction == null ? "DESC" : direction),
                                sortBy);
        int resolvedSize = (size == null || size < 1 || size >= 50) ? defaultSize : size;

        return PageRequest.of((page == null || page < 1) ? 0 : page - 1, resolvedSize, sort);
    }
}
