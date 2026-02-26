package com.sipomeokjo.commitme.api.pagination;

public record CursorRequest(String next, Integer size) {
    public int limit(int defaultSize) {
        return (size == null || size < 1 || size >= 50) ? defaultSize : size;
    }

    public static int resolveLimit(CursorRequest request, int defaultSize) {
        return request == null ? defaultSize : request.limit(defaultSize);
    }
}
