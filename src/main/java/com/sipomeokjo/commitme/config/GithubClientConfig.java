package com.sipomeokjo.commitme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubClientConfig {
	
	@Bean
	RestClient githubApiClient(RestClient.Builder builder) {
		return builder
				.baseUrl("https://api.github.com")
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("X-GitHub-Api-Version", "2022-11-28")
				.build();
	}
	
	@Bean
	RestClient githubOAuthClient(RestClient.Builder builder) {
		return builder
				.baseUrl("https://github.com")
				.defaultHeader("Accept", "application/json")
				.build();
	}
}
