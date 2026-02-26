package com.sipomeokjo.commitme.domain.loadtest.auth.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.LoadtestMockAuthProperties;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.auth.service.AuthSessionIssueService;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthBulkCreateItemResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthBulkCreateRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthBulkCreateResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLoginRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLoginResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLogoutRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthLogoutResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthResetRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthResetResponse;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthSignupRequest;
import com.sipomeokjo.commitme.domain.loadtest.auth.dto.LoadtestAuthSignupResponse;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import com.sipomeokjo.commitme.domain.userSetting.repository.UserSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoadtestAuthService {

    private static final AuthProvider MOCK_PROVIDER = AuthProvider.GITHUB;
    private static final String MOCK_PREFIX = "lt";
    private static final int MAX_RESET_TARGET_COUNT = 5_000;
    private static final String ACTIVE_USER_NAME_PREFIX = "lt";
    private static final int ACTIVE_USER_NAME_MAX_LENGTH = 10;
    private static final String DEFAULT_POLICY_DOCUMENT = "loadtest-mock";
    private static final String DEFAULT_POLICY_VERSION = "loadtest-v1";

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PositionRepository positionRepository;
    private final PolicyAgreementRepository policyAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthSessionIssueService authSessionIssueService;
    private final LoadtestMockAuthProperties loadtestMockAuthProperties;
    private final Clock clock;

    public LoadtestAuthSignupResponse signupPending(LoadtestAuthSignupRequest request) {
        MockIdentity identity = buildIdentity(request.runId(), request.userKey());
        CreatedMockUser created = createMockUser(identity, UserStatus.PENDING);
        return new LoadtestAuthSignupResponse(
                created.user().getId(),
                created.user().getStatus(),
                created.auth().getProviderUserId(),
                created.auth().getProviderUsername());
    }

    public LoadtestAuthBulkCreateResponse bulkCreate(LoadtestAuthBulkCreateRequest request) {
        String runId = normalizeRunId(request.runId());
        int count = normalizeBulkCount(request.count());
        int startIndex = request.startIndex() == null ? 1 : request.startIndex();
        if (startIndex <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        UserStatus status = normalizeBulkStatus(request.status());
        boolean returnToken = Boolean.TRUE.equals(request.returnToken());

        List<LoadtestAuthBulkCreateItemResponse> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int sequence = startIndex + i;
            MockIdentity identity = buildIdentity(runId, "u" + sequence);
            CreatedMockUser created = createMockUser(identity, status);
            SessionTokens tokens = returnToken ? issueSessionTokens(created.user()) : null;

            items.add(
                    new LoadtestAuthBulkCreateItemResponse(
                            created.user().getId(),
                            created.user().getStatus(),
                            created.auth().getProviderUserId(),
                            created.auth().getProviderUsername(),
                            tokens == null ? null : tokens.accessToken(),
                            tokens == null ? null : tokens.refreshToken()));
        }

        return new LoadtestAuthBulkCreateResponse(
                runId, count, items.size(), status, returnToken, items);
    }

    public LoadtestAuthLoginResponse login(LoadtestAuthLoginRequest request) {
        String providerUserId = normalizeProviderUserId(request.providerUserId());
        boolean returnToken = Boolean.TRUE.equals(request.returnToken());

        Auth auth =
                authRepository
                        .findByProviderAndProviderUserId(MOCK_PROVIDER, providerUserId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        ensureMockAuth(auth);
        User user = auth.getUser();
        SessionTokens tokens = issueSessionTokens(user);

        return new LoadtestAuthLoginResponse(
                user.getId(),
                user.getStatus(),
                auth.getProviderUserId(),
                auth.getProviderUsername(),
                user.getStatus() == UserStatus.ACTIVE,
                returnToken ? tokens.accessToken() : null,
                returnToken ? tokens.refreshToken() : null,
                tokens.accessToken(),
                tokens.refreshToken());
    }

    public LoadtestAuthLogoutResponse logout(LoadtestAuthLogoutRequest request) {
        Auth auth = resolveAuthForLogout(request);
        ensureMockAuth(auth);
        User user = auth.getUser();
        int revokedCount = revokeAllRefreshTokens(user.getId());
        return new LoadtestAuthLogoutResponse(user.getId(), auth.getProviderUserId(), revokedCount);
    }

    public LoadtestAuthResetResponse reset(LoadtestAuthResetRequest request) {
        String runId = normalizeRunId(request.runId());
        int limit = normalizeResetLimit(request.limit());
        String providerUserIdPrefix = buildProviderUserIdPrefix(runId);

        List<Auth> matched =
                authRepository.findAllByProviderAndProviderUserIdStartingWith(
                        MOCK_PROVIDER, providerUserIdPrefix);
        int matchedCount = matched.size();
        int processedCount = 0;
        int deactivatedUserCount = 0;
        int revokedRefreshTokenCount = 0;
        List<Long> resetTargetUserIds = new ArrayList<>(Math.min(matchedCount, limit));

        Instant now = Instant.now(clock);
        LocalDateTime refreshTokenRevokedAt = LocalDateTime.now(clock);
        for (Auth auth : matched) {
            if (processedCount >= limit) {
                break;
            }
            User user = auth.getUser();
            Long userId = user.getId();
            auth.clearSensitiveInfo();
            if (user.getStatus() != UserStatus.INACTIVE) {
                user.deactivate(now);
                deactivatedUserCount++;
            }
            resetTargetUserIds.add(userId);
            processedCount++;
        }

        for (Long userId : resetTargetUserIds) {
            revokedRefreshTokenCount += revokeAllRefreshTokens(userId, refreshTokenRevokedAt);
        }

        log.info(
                "[LoadtestAuth][Reset] runId={}, matchedCount={}, processedCount={}, deactivatedUserCount={}, revokedRefreshTokenCount={}",
                runId,
                matchedCount,
                processedCount,
                deactivatedUserCount,
                revokedRefreshTokenCount);

        return new LoadtestAuthResetResponse(
                runId,
                matchedCount,
                processedCount,
                deactivatedUserCount,
                revokedRefreshTokenCount);
    }

    private CreatedMockUser createMockUser(MockIdentity identity, UserStatus status) {
        authRepository
                .findByProviderAndProviderUserId(MOCK_PROVIDER, identity.providerUserId())
                .ifPresent(
                        ignored -> {
                            throw new BusinessException(ErrorCode.BAD_REQUEST);
                        });

        User user = userRepository.save(User.builder().status(status).build());
        userSettingRepository.save(UserSetting.defaultSetting(user));
        applyMockFixtureIfNeeded(user, identity, status);

        Auth auth =
                authRepository.save(
                        Auth.builder()
                                .user(user)
                                .provider(MOCK_PROVIDER)
                                .providerUserId(identity.providerUserId())
                                .providerUsername(identity.providerUsername())
                                .accessToken(null)
                                .tokenScopes(null)
                                .tokenExpiresAt(null)
                                .build());

        return new CreatedMockUser(user, auth);
    }

    private void applyMockFixtureIfNeeded(User user, MockIdentity identity, UserStatus status) {
        if (status != UserStatus.ACTIVE) {
            return;
        }

        Position defaultPosition = resolveDefaultPositionForActiveMock();
        user.updateOnboarding(
                defaultPosition, buildActiveMockName(identity), null, null, UserStatus.ACTIVE);
        savePrivacyAgreement(user);
    }

    private Position resolveDefaultPositionForActiveMock() {
        Long defaultPositionId = loadtestMockAuthProperties.getDefaultPositionId();
        if (defaultPositionId == null || defaultPositionId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return positionRepository
                .findById(defaultPositionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    }

    private String buildActiveMockName(MockIdentity identity) {
        String seed = identity.providerUsername().replaceAll("[^A-Za-z0-9]", "");
        if (seed.isBlank()) {
            seed = "user";
        }
        int suffixLength = ACTIVE_USER_NAME_MAX_LENGTH - ACTIVE_USER_NAME_PREFIX.length();
        String suffix =
                seed.length() > suffixLength ? seed.substring(seed.length() - suffixLength) : seed;
        return ACTIVE_USER_NAME_PREFIX + suffix;
    }

    private void savePrivacyAgreement(User user) {
        policyAgreementRepository.save(
                PolicyAgreement.builder()
                        .user(user)
                        .document(DEFAULT_POLICY_DOCUMENT)
                        .policyType(PolicyType.PRIVACY)
                        .policyVersion(DEFAULT_POLICY_VERSION)
                        .agreedAt(LocalDateTime.now(clock))
                        .build());
    }

    private SessionTokens issueSessionTokens(User user) {
        AuthTokenReissueResult issued =
                authSessionIssueService.issueTokens(user.getId(), user.getStatus());
        return new SessionTokens(issued.accessToken(), issued.refreshToken());
    }

    private int revokeAllRefreshTokens(Long userId) {
        return revokeAllRefreshTokens(userId, LocalDateTime.now(clock));
    }

    private int revokeAllRefreshTokens(Long userId, LocalDateTime revokedAt) {
        int activeTokenCount = refreshTokenRepository.countActiveByUserId(userId, revokedAt);
        if (activeTokenCount <= 0) {
            return 0;
        }

        refreshTokenRepository.revokeAllByUserId(userId, revokedAt);
        return activeTokenCount;
    }

    private Auth resolveAuthForLogout(LoadtestAuthLogoutRequest request) {
        Long userId = request.userId();
        String providerUserId = request.providerUserId();

        if (userId == null && (providerUserId == null || providerUserId.isBlank())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Auth auth = null;
        if (providerUserId != null && !providerUserId.isBlank()) {
            auth =
                    authRepository
                            .findByProviderAndProviderUserId(
                                    MOCK_PROVIDER, normalizeProviderUserId(providerUserId))
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }

        if (userId != null) {
            Auth byUserId =
                    authRepository
                            .findByUser_IdAndProvider(userId, MOCK_PROVIDER)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            if (auth != null && !byUserId.getId().equals(auth.getId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            auth = byUserId;
        }

        if (auth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return auth;
    }

    private void ensureMockAuth(Auth auth) {
        if (auth == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String providerUserId = auth.getProviderUserId();
        if (providerUserId == null || !providerUserId.startsWith(MOCK_PREFIX + ":")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private int normalizeBulkCount(Integer count) {
        int maxBulkCount = Math.max(1, loadtestMockAuthProperties.getMaxBulkCount());
        if (count == null || count <= 0 || count > maxBulkCount) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return count;
    }

    private int normalizeResetLimit(Integer limit) {
        if (limit == null) {
            return MAX_RESET_TARGET_COUNT;
        }
        if (limit <= 0 || limit > MAX_RESET_TARGET_COUNT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return limit;
    }

    private UserStatus normalizeBulkStatus(UserStatus status) {
        UserStatus normalized = status == null ? UserStatus.PENDING : status;
        if (normalized != UserStatus.PENDING && normalized != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private MockIdentity buildIdentity(String runId, String userKey) {
        String normalizedRunId = normalizeRunId(runId);
        String normalizedUserKey = normalizeUserKey(userKey);
        return new MockIdentity(
                buildProviderUserId(normalizedRunId, normalizedUserKey),
                buildProviderUsername(normalizedRunId, normalizedUserKey));
    }

    private String buildProviderUserIdPrefix(String runId) {
        return MOCK_PREFIX + ":" + runId + ":";
    }

    private String buildProviderUserId(String runId, String userKey) {
        return buildProviderUserIdPrefix(runId) + userKey;
    }

    private String buildProviderUsername(String runId, String userKey) {
        return MOCK_PREFIX + "_" + runId + "_" + userKey;
    }

    private String normalizeProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return providerUserId.trim();
    }

    private String normalizeRunId(String runId) {
        return normalizeIdentifier(runId, 64);
    }

    private String normalizeUserKey(String userKey) {
        return normalizeIdentifier(userKey, 64);
    }

    private String normalizeIdentifier(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean allowed =
                    (c >= 'a' && c <= 'z')
                            || (c >= 'A' && c <= 'Z')
                            || (c >= '0' && c <= '9')
                            || c == '-'
                            || c == '_'
                            || c == '.';
            if (!allowed) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
        }
        return trimmed;
    }

    private record MockIdentity(String providerUserId, String providerUsername) {}

    private record CreatedMockUser(User user, Auth auth) {}

    private record SessionTokens(String accessToken, String refreshToken) {}
}
