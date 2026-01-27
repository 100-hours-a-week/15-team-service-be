package com.sipomeokjo.commitme.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUserResponse(
		Long id,
		String login,
		String name,
		String email,
		@JsonProperty("avatar_url") String avatarUrl
) {
}
