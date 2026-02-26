package com.sipomeokjo.commitme.domain.loadtest.auth.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record LoadtestAuthBulkCreateRequest(
        String runId, Integer count, Integer startIndex, UserStatus status, Boolean returnToken) {}
