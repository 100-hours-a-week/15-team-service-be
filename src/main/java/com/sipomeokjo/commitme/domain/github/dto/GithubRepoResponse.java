package com.sipomeokjo.commitme.domain.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepoResponse(
        String name,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("updated_at") String updatedAt
) {}
