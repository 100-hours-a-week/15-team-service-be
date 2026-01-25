package com.sipomeokjo.commitme.domain.github.dto;

public record RepoSummaryDto(
        String name,
        String repoUrl,
        boolean isPrivate,
        String updatedAt
) {}
