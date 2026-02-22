package com.sipomeokjo.commitme.domain.notification.dto;

import java.util.List;

public record NotificationListResponse(
        Long latestId, List<NotificationListItem> items, Long nextCursor, boolean hasNext) {}
