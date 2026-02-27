package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.config.GithubProperties;
import com.sipomeokjo.commitme.domain.auth.dto.AuthLoginResult;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.auth.dto.GithubAccessTokenResponse;
import com.sipomeokjo.commitme.domain.auth.dto.GithubUserResponse;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import com.sipomeokjo.commitme.domain.userSetting.repository.UserSettingRepository;
import com.sipomeokjo.commitme.security.jwt.AccessTokenCipher;
import com.sipomeokjo.commitme.security.jwt.RefreshTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthCommandService {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheService refreshTokenCacheService;
    private final AuthSessionIssueService authSessionIssueService;
    private final AccessTokenCipher accessTokenCipher;
    private final RefreshTokenProvider refreshTokenProvider;
    private final GithubProperties githubProperties;
    private final Clock clock;
    private final RestClient githubOAuthClient;
    private final RestClient githubApiClient;

    private static final Pattern SCOPE_WHITESPACE = Pattern.compile("\\s+");

    public AuthLoginResult loginWithGithub(String code) {
        log.info("[Auth][GithubLogin] start codeFingerprint={}", fingerprint(code));
        GithubAccessTokenResponse tokenResponse = exchangeToken(code);
        if (tokenResponse.accessToken() == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }

        GithubUserResponse githubUser = fetchUser(tokenResponse.accessToken());
        if (githubUser == null || githubUser.id() == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }

        Auth auth =
                authRepository
                        .findByProviderAndProviderUserId(
                                AuthProvider.GITHUB, String.valueOf(githubUser.id()))
                        .orElse(null);

        User user;
        if (auth == null) {
            user = userRepository.save(User.builder().status(UserStatus.PENDING).build());
            userSettingRepository.save(UserSetting.defaultSetting(user));
            Auth newAuth =
                    Auth.builder()
                            .user(user)
                            .provider(AuthProvider.GITHUB)
                            .providerUserId(String.valueOf(githubUser.id()))
                            .providerUsername(githubUser.login())
                            .accessToken(accessTokenCipher.encrypt(tokenResponse.accessToken()))
                            .tokenScopes(normalizeScopes(tokenResponse.scope()))
                            .tokenExpiresAt(null)
                            .build();
            authRepository.save(newAuth);
        } else {
            user = auth.getUser();
            auth.updateTokenInfo(
                    githubUser.login(),
                    accessTokenCipher.encrypt(tokenResponse.accessToken()),
                    normalizeScopes(tokenResponse.scope()),
                    null);
        }

        AuthTokenReissueResult tokenResult =
                authSessionIssueService.issueTokens(user.getId(), user.getStatus());
        log.info(
                "[Auth][GithubLogin] success userId={} status={} accessTokenFingerprint={} refreshTokenFingerprint={}",
                user.getId(),
                user.getStatus(),
                fingerprint(tokenResult.accessToken()),
                fingerprint(tokenResult.refreshToken()));

        return new AuthLoginResult(
                tokenResult.accessToken(),
                tokenResult.refreshToken(),
                user.getStatus() == UserStatus.ACTIVE);
    }

    public AuthTokenReissueResult reissueAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return reissueAccessTokenInternal(refreshToken);
    }

    public AuthTokenReissueResult reissueAccessToken(List<String> refreshTokenCandidates) {
        if (refreshTokenCandidates == null || refreshTokenCandidates.isEmpty()) {
            log.warn("[Auth][TokenReissue] empty_refresh_token_candidates");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        log.info(
                "[Auth][TokenReissue] candidate_scan_start count={} fingerprints={}",
                refreshTokenCandidates.size(),
                refreshTokenCandidates.stream().map(this::fingerprint).toList());
        for (String candidate : refreshTokenCandidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                log.info(
                        "[Auth][TokenReissue] candidate_try fingerprint={}",
                        fingerprint(candidate));
                return reissueAccessTokenInternal(candidate);
            } catch (BusinessException ex) {
                if (ex.getErrorCode() != ErrorCode.REFRESH_TOKEN_INVALID) {
                    log.warn(
                            "[Auth][TokenReissue] candidate_non_refresh_error fingerprint={} code={}",
                            fingerprint(candidate),
                            ex.getErrorCode().name());
                    throw ex;
                }
                log.warn(
                        "[Auth][TokenReissue] candidate_invalid fingerprint={}",
                        fingerprint(candidate));
            }
        }
        log.warn("[Auth][TokenReissue] all_candidates_invalid");
        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    private AuthTokenReissueResult reissueAccessTokenInternal(String refreshToken) {
        String tokenHash = refreshTokenProvider.hash(refreshToken);
        Instant now = Instant.now(clock);
        var cached =
                refreshTokenCacheService
                        .get(tokenHash)
                        .filter(value -> value.getExpiresAt().isAfter(now))
                        .orElse(null);
        if (cached != null) {
            boolean revoked = authSessionIssueService.revokeRefreshToken(refreshToken);
            if (!revoked) {
                log.warn(
                        "[Auth][TokenReissue] cached_token_revoke_failed tokenHashFingerprint={}",
                        fingerprint(tokenHash));
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            UserStatus status = resolveCurrentUserStatus(cached.getUserId());
            log.info(
                    "[Auth][TokenReissue] cache_hit userId={} status={}",
                    cached.getUserId(),
                    status);
            return authSessionIssueService.issueTokens(cached.getUserId(), status);
        }

        refreshTokenCacheService.evict(tokenHash);
        RefreshToken refreshTokenEntity =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshTokenEntity.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (!refreshTokenEntity.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = refreshTokenEntity.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshTokenEntity.revoke(now);
        log.info("[Auth][TokenReissue] db_hit userId={} status={}", user.getId(), user.getStatus());
        return authSessionIssueService.issueTokens(user.getId(), user.getStatus());
    }

    private GithubAccessTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubProperties.getClientId());
        form.add("client_secret", githubProperties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", githubProperties.getRedirectUri());

        GithubAccessTokenResponse response =
                githubOAuthClient
                        .post()
                        .uri("/login/oauth/access_token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(form)
                        .retrieve()
                        .body(GithubAccessTokenResponse.class);
        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            log.warn("[Auth][TokenExchange] 응답 없음 또는 access_token 누락: response={}", response);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return response;
    }

    private GithubUserResponse fetchUser(String accessToken) {
        GithubUserResponse response =
                githubApiClient
                        .get()
                        .uri("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(GithubUserResponse.class);
        if (response == null || response.id() == null) {
            log.warn("[Auth][FetchUser] 사용자 조회 실패: 사유=응답 없음 또는 id 누락, response={}", response);
        }
        return response;
    }

    private UserStatus resolveCurrentUserStatus(Long userId) {
        return userRepository
                .findById(userId)
                .map(User::getStatus)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    private String normalizeScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        return SCOPE_WHITESPACE.matcher(scope.trim()).replaceAll(" ");
    }

    private String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "empty";
        }
        int visible = Math.min(6, value.length());
        return "***" + value.substring(value.length() - visible);
    }
}
