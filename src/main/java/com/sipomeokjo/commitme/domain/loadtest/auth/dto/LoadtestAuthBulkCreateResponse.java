package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.util.List;

public record LoadtestAuthBulkCreateResponse(
        String runId,
        int requestedCount,
        int createdCount,
        UserStatus status,
        boolean returnToken,
        List<LoadtestAuthBulkCreateItemResponse> items) {}
