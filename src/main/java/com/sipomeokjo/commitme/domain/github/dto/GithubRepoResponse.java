package com.sipomeokjo.commitme.domain.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepoResponse(
        String name,
        String description,
        String language,
        boolean fork,
        ParentRepo parent,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("updated_at") String updatedAt) {

    public record ParentRepo(String language) {}

    public String effectiveLanguage() {
        if (language != null) {
            return language;
        }
        if (fork && parent != null) {
            return parent.language();
        }
        return null;
    }
}
