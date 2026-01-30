package com.sipomeokjo.commitme.domain.github.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.github.dto.GithubRepoResponse;
import com.sipomeokjo.commitme.domain.github.dto.RepoSummaryDto;
import com.sipomeokjo.commitme.security.jwt.AccessTokenCipher;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GithubRepositoryService {
	
	private final RestClient githubApiClient;
	private final AuthRepository authRepository;
	private final AccessTokenCipher accessTokenCipher;
	
	public List<RepoSummaryDto> listMyRepos(Long userId) {
		Auth auth =
				authRepository
						.findByUser_IdAndProvider(userId, AuthProvider.GITHUB)
						.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		
		String githubToken = accessTokenCipher.decrypt(auth.getAccessToken());
		
		GithubRepoResponse[] repos =
				githubApiClient
						.get()
						.uri(
								uriBuilder ->
										uriBuilder
												.path("/user/repos")
												.queryParam("per_page", 100)
												.queryParam("sort", "updated")
												.build())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken)
						.retrieve()
						.body(GithubRepoResponse[].class);
		
		if (repos == null) return List.of();
		
		return Arrays.stream(repos)
				.map(r -> new RepoSummaryDto(r.name(), r.htmlUrl(), r.isPrivate(), r.updatedAt()))
				.collect(Collectors.toList());
	}
}
