package com.sipomeokjo.commitme.api.pagination;

import java.util.List;

public record CursorResponse<T>(List<T> chats, String before, String next) {}
