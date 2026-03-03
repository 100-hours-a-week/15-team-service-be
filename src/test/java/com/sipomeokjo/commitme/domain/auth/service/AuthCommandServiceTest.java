package com.sipomeokjo.commitme.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthCommandServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserSettingRepository userSettingRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenCacheService refreshTokenCacheService;
    @Mock private AuthSessionIssueService authSessionIssueService;
    @Mock private AccessTokenCipher accessTokenCipher;
    @Mock private RefreshTokenProvider refreshTokenProvider;
    @Mock private GithubProperties githubProperties;
    @Mock private RestClient githubOAuthClient;
    @Mock private RestClient githubApiClient;
    @Mock private RestClient.RequestBodyUriSpec oauthPostSpec;
    @Mock private RestClient.RequestBodySpec oauthBodySpec;
    @Mock private RestClient.RequestHeadersUriSpec apiGetSpec;
    @Mock private RestClient.RequestHeadersSpec apiHeadersSpec;
    @Mock private RestClient.ResponseSpec oauthResponseSpec;
    @Mock private RestClient.ResponseSpec apiResponseSpec;

    private Clock clock;
    private AuthCommandService authCommandService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
        authCommandService =
                new AuthCommandService(
                        authRepository,
                        userRepository,
                        userSettingRepository,
                        refreshTokenRepository,
                        refreshTokenCacheService,
                        authSessionIssueService,
                        accessTokenCipher,
                        refreshTokenProvider,
                        githubProperties,
                        clock,
                        githubOAuthClient,
                        githubApiClient);

        given(githubProperties.getClientId()).willReturn("client-id");
        given(githubProperties.getClientSecret()).willReturn("client-secret");
        given(githubProperties.getRedirectUri()).willReturn("https://example.com/callback");
        given(accessTokenCipher.encrypt(anyString())).willReturn("enc-token");
    }

    @Test
    void loginWithGithub_inactiveUserBeforeOneDay_throwsWithdrawn() {
        User inactiveUser =
                User.builder()
                        .id(10L)
                        .status(UserStatus.INACTIVE)
                        .deletedAt(Instant.parse("2026-02-28T12:00:01Z"))
                        .build();
        Auth auth =
                Auth.builder()
                        .id(1L)
                        .provider(AuthProvider.GITHUB)
                        .providerUserId("100")
                        .user(inactiveUser)
                        .build();

        stubGithubExchangeAndFetch("oauth-access-token", 100L, "octocat");
        given(authRepository.findByProviderAndProviderUserIdWithLock(AuthProvider.GITHUB, "100"))
                .willReturn(Optional.of(auth));

        assertThatThrownBy(() -> authCommandService.loginWithGithub("code"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OAUTH_ACCOUNT_WITHDRAWN);
    }

    @Test
    void loginWithGithub_inactiveUserAfterOneDay_createsNewPendingUserAndRebindsAuth() {
        User oldUser =
                User.builder()
                        .id(10L)
                        .status(UserStatus.INACTIVE)
                        .deletedAt(Instant.parse("2026-02-27T23:59:59Z"))
                        .build();
        Auth auth =
                Auth.builder()
                        .id(1L)
                        .provider(AuthProvider.GITHUB)
                        .providerUserId("200")
                        .providerUsername("old-name")
                        .user(oldUser)
                        .build();

        stubGithubExchangeAndFetch("oauth-access-token", 200L, "new-name");
        given(authRepository.findByProviderAndProviderUserIdWithLock(AuthProvider.GITHUB, "200"))
                .willReturn(Optional.of(auth));

        AtomicLong userIdSequence = new AtomicLong(1000L);
        given(userRepository.save(any(User.class)))
                .willAnswer(
                        invocation -> {
                            User source = invocation.getArgument(0);
                            return User.builder()
                                    .id(userIdSequence.incrementAndGet())
                                    .status(source.getStatus())
                                    .deletedAt(source.getDeletedAt())
                                    .build();
                        });
        given(userSettingRepository.save(any(UserSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(authSessionIssueService.issueTokens(eq(1001L), eq(UserStatus.PENDING)))
                .willReturn(new AuthTokenReissueResult("new-access", "new-refresh"));

        AuthLoginResult result = authCommandService.loginWithGithub("code");

        assertThat(result.onboardingCompleted()).isFalse();
        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(auth.getUser().getId()).isEqualTo(1001L);
        assertThat(auth.getUser().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(oldUser.getId()).isEqualTo(10L);
        assertThat(oldUser.getStatus()).isEqualTo(UserStatus.INACTIVE);

        verify(userSettingRepository).save(any(UserSetting.class));
        verify(authSessionIssueService).issueTokens(1001L, UserStatus.PENDING);
    }

    private void stubGithubExchangeAndFetch(String accessToken, Long githubUserId, String login) {
        given(githubOAuthClient.post()).willReturn(oauthPostSpec);
        given(oauthPostSpec.uri("/login/oauth/access_token")).willReturn(oauthBodySpec);
        given(oauthBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .willReturn(oauthBodySpec);
        given(oauthBodySpec.accept(MediaType.APPLICATION_JSON)).willReturn(oauthBodySpec);
        doReturn(oauthBodySpec).when(oauthBodySpec).body(any(MultiValueMap.class));
        given(oauthBodySpec.retrieve()).willReturn(oauthResponseSpec);
        given(oauthResponseSpec.body(GithubAccessTokenResponse.class))
                .willReturn(new GithubAccessTokenResponse(accessToken, "bearer", "repo user"));

        given(githubApiClient.get()).willReturn(apiGetSpec);
        given(apiGetSpec.uri("/user")).willReturn(apiHeadersSpec);
        given(apiHeadersSpec.header("Authorization", "Bearer " + accessToken))
                .willReturn(apiHeadersSpec);
        given(apiHeadersSpec.retrieve()).willReturn(apiResponseSpec);
        given(apiResponseSpec.body(GithubUserResponse.class))
                .willReturn(new GithubUserResponse(githubUserId, login, null, null, null));
    }
}
