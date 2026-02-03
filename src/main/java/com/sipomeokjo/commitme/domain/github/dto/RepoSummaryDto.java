package com.sipomeokjo.commitme.domain.github.dto;

public record RepoSummaryDto(
        String name,
        String description,
        String language,
        String repoUrl,
        boolean isPrivate,
        String updatedAt) {}
