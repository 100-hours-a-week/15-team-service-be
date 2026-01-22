package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.config.GithubProperties;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.dto.GithubAccessTokenResponse;
import com.sipomeokjo.commitme.domain.auth.dto.GithubUserResponse;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.security.AccessTokenProvider;
import com.sipomeokjo.commitme.security.JwtProperties;
import com.sipomeokjo.commitme.security.RefreshTokenProvider;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class AuthCommandService {
	
	private final AuthRepository authRepository;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccessTokenProvider accessTokenProvider;
	private final RefreshTokenProvider refreshTokenProvider;
	private final GithubProperties githubProperties;
	private final JwtProperties jwtProperties;
	private final RestClient githubOAuthClient;
	private final RestClient githubApiClient;
	private static final Pattern SCOPE_WHITESPACE = Pattern.compile("\\s+");

	public AuthCommandService(
			AuthRepository authRepository,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			AccessTokenProvider accessTokenProvider,
			RefreshTokenProvider refreshTokenProvider,
			GithubProperties githubProperties,
			JwtProperties jwtProperties,
			@Qualifier("githubOAuthClient") RestClient githubOAuthClient,
			@Qualifier("githubApiClient") RestClient githubApiClient
	) {
		this.authRepository = authRepository;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.accessTokenProvider = accessTokenProvider;
		this.refreshTokenProvider = refreshTokenProvider;
		this.githubProperties = githubProperties;
		this.jwtProperties = jwtProperties;
		this.githubOAuthClient = githubOAuthClient;
		this.githubApiClient = githubApiClient;
	}
	
	public AuthLoginResult loginWithGithub(String code) {
		GithubAccessTokenResponse tokenResponse = exchangeToken(code);
		if (tokenResponse.accessToken() == null) {
			throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		}

		GithubUserResponse githubUser = fetchUser(tokenResponse.accessToken());
		if (githubUser == null || githubUser.id() == null) {
			throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		}

		Auth auth = authRepository.findByProviderAndProviderUserId(
				AuthProvider.GITHUB, String.valueOf(githubUser.id()))
				.orElse(null);

		User user;
		if (auth == null) {
			user = userRepository.save(User.builder()
					.status(UserStatus.PENDING)
					.build());
			Auth newAuth = Auth.builder()
					.user(user)
					.provider(AuthProvider.GITHUB)
					.providerUserId(String.valueOf(githubUser.id()))
					.providerUsername(githubUser.login())
					.accessToken(tokenResponse.accessToken())
					.tokenScopes(normalizeScopes(tokenResponse.scope()))
					.tokenExpiresAt(null)
					.build();
			authRepository.save(newAuth);
		} else {
			user = auth.getUser();
			auth.updateTokenInfo(
					githubUser.login(),
					tokenResponse.accessToken(),
					normalizeScopes(tokenResponse.scope()),
					null
			);
		}

		String accessToken = accessTokenProvider.createAccessToken(user.getId(), user.getStatus());
		String refreshToken = refreshTokenProvider.generateRawToken();
		String refreshTokenHash = refreshTokenProvider.hash(refreshToken);

		RefreshToken refreshTokenEntity = RefreshToken.builder()
				.user(user)
				.tokenHash(refreshTokenHash)
				.expiresAt(LocalDateTime.now().plus(jwtProperties.getRefreshExpiration()))
				.revokedAt(null)
				.build();
		refreshTokenRepository.save(refreshTokenEntity);

		return new AuthLoginResult(accessToken, refreshToken, user.getStatus() == UserStatus.ACTIVE);
	}
	
	private GithubAccessTokenResponse exchangeToken(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", githubProperties.getClientId());
		form.add("client_secret", githubProperties.getClientSecret());
		form.add("code", code);
		form.add("redirect_uri", githubProperties.getRedirectUri());

		GithubAccessTokenResponse response = githubOAuthClient.post()
				.uri("/login/oauth/access_token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.APPLICATION_JSON)
				.body(form)
				.retrieve()
				.body(GithubAccessTokenResponse.class);
		if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
			log.warn("[Auth][TokenExchange] 응답 없음 또는 access_token 누락: response={}", response);
			throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
		}
		return response;
	}

	private GithubUserResponse fetchUser(String accessToken) {
		GithubUserResponse response = githubApiClient.get()
				.uri("/user")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(GithubUserResponse.class);
		if (response == null || response.id() == null) {
			log.warn("[Auth][FetchUser] 사용자 조회 실패: 사유=응답 없음 또는 id 누락, response={}", response);
		}
		return response;
	}

	private String normalizeScopes(String scope) {
		if (scope == null || scope.isBlank()) {
			return null;
		}
		return SCOPE_WHITESPACE.matcher(scope.trim()).replaceAll(" ");
	}
}
