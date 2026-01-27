package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.domain.auth.config.GithubProperties;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {
	
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
	
	private final GithubProperties githubProperties;
	
	public String getLoginUrl(String state) {
		log.info("[Auth][LoginUrl] 로그인 URL 생성: 사유=설정값 기반, clientId={}, redirectUri={}, scope={}",
				githubProperties.getClientId(),
				githubProperties.getRedirectUri(),
				githubProperties.getScope());
		return UriComponentsBuilder.fromUriString(GITHUB_AUTHORIZE_URL)
				.queryParam("client_id", githubProperties.getClientId())
				.queryParam("redirect_uri", githubProperties.getRedirectUri())
				.queryParam("scope", githubProperties.getScope())
				.queryParam("state", state)
				.build()
				.encode()
				.toUriString();
	}
	
	public String generateState() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
