package com.sipomeokjo.commitme.domain.loadtest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.LoadtestProperties;
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
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCacheEvictRequest;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCacheEvictResponse;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCleanupRequest;
import com.sipomeokjo.commitme.domain.loadtest.dto.LoadtestCleanupResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeBulkSeedRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeBulkSeedResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeCallbackReplayRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeCallbackReplayResponse;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeCallbackType;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeReplayResultStatus;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeResetRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeResetResponse;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeCallbackRequest;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import com.sipomeokjo.commitme.domain.userSetting.repository.UserSettingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadtestService {

    private static final AuthProvider MOCK_PROVIDER = AuthProvider.GITHUB;
    private static final String MOCK_PREFIX = "lt";
    private static final int MAX_RESET_TARGET_COUNT = 5_000;
    private static final int MAX_SEED_USER_COUNT = 1_000;
    private static final int MAX_RESUMES_PER_USER = 30;
    private static final int MAX_VERSIONS_PER_RESUME = 20;
    private static final int MAX_CALLBACK_REPLAY_LIMIT = 1_000;
    private static final int MAX_CALLBACK_DUPLICATE_COUNT = 20;
    private static final String ACTIVE_USER_NAME_PREFIX = "lt";
    private static final int ACTIVE_USER_NAME_MAX_LENGTH = 10;
    private static final String DEFAULT_POLICY_DOCUMENT = "loadtest-mock";
    private static final String DEFAULT_POLICY_VERSION = "loadtest-v1";
    private static final String LOADTEST_SEED_ERROR_CODE = "LOADTEST_SEED";
    private static final String SUPPORTED_CACHE_NAME_POSITIONS = "positions";

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final PositionRepository positionRepository;
    private final PolicyAgreementRepository policyAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheService refreshTokenCacheService;
    private final AuthSessionIssueService authSessionIssueService;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeAiCallbackService resumeAiCallbackService;
    private final NotificationRepository notificationRepository;
    private final LoadtestProperties loadtestProperties;
    private final CacheManager cacheManager;
    private final RestClient aiClient;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final Clock clock;

    @Transactional
    public LoadtestAuthSignupResponse signupPending(LoadtestAuthSignupRequest request) {
        MockIdentity identity = buildIdentity(request.runId(), request.userKey());
        CreatedMockUser created = createMockUser(identity, UserStatus.PENDING);
        return new LoadtestAuthSignupResponse(
                created.user().getId(),
                created.user().getStatus(),
                created.auth().getProviderUserId(),
                created.auth().getProviderUsername());
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public LoadtestAuthLogoutResponse logout(LoadtestAuthLogoutRequest request) {
        Auth auth = resolveAuthForLogout(request);
        ensureMockAuth(auth);
        User user = auth.getUser();
        int revokedCount = revokeAllRefreshTokens(user.getId());
        return new LoadtestAuthLogoutResponse(user.getId(), auth.getProviderUserId(), revokedCount);
    }

    @Transactional
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
            revokedRefreshTokenCount += revokeAllRefreshTokens(userId, now);
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

    @Transactional
    public LoadtestCleanupResponse cleanup(LoadtestCleanupRequest request) {
        String runId = normalizeRunId(request.runId());
        boolean deleteResumes = request.deleteResumes() == null || request.deleteResumes();
        boolean deleteNotifications =
                request.deleteNotifications() == null || request.deleteNotifications();
        boolean deleteUsers = request.deleteUsers() == null || request.deleteUsers();

        List<Auth> matchedAuths =
                authRepository.findAllByProviderAndProviderUserIdStartingWith(
                        MOCK_PROVIDER, buildProviderUserIdPrefix(runId));
        List<Long> userIds =
                matchedAuths.stream().map(auth -> auth.getUser().getId()).distinct().toList();

        if (userIds.isEmpty()) {
            return new LoadtestCleanupResponse(runId, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int deletedResumeCount = 0;
        int deletedVersionCount = 0;
        int deletedNotificationCount = 0;
        int deletedRefreshTokenCount = 0;
        int deletedPolicyAgreementCount = 0;
        int deletedUserSettingCount = 0;
        int deletedAuthCount = 0;
        int deletedUserCount = 0;

        List<Resume> resumes = resumeRepository.findByUser_IdIn(userIds);
        List<Long> resumeIds = resumes.stream().map(Resume::getId).toList();

        if (deleteResumes && !resumeIds.isEmpty()) {
            deletedVersionCount = resumeVersionRepository.findAllByResume_IdIn(resumeIds).size();
            resumeVersionRepository.deleteByResume_IdIn(resumeIds);
            deletedResumeCount = resumes.size();
            resumeRepository.deleteAllInBatch(resumes);
        }

        if (deleteNotifications || deleteUsers) {
            deletedNotificationCount = notificationRepository.deleteAllByUserIds(userIds);
        }

        if (deleteUsers) {
            List<String> tokenHashes = refreshTokenRepository.findTokenHashesByUserIds(userIds);
            deletedRefreshTokenCount = refreshTokenRepository.deleteAllByUserIds(userIds);
            refreshTokenCacheService.evictAll(tokenHashes);

            deletedPolicyAgreementCount = policyAgreementRepository.deleteAllByUserIds(userIds);
            deletedUserSettingCount = userSettingRepository.deleteAllByUserIds(userIds);
            deletedAuthCount = authRepository.deleteAllByUserIds(userIds);
            deletedUserCount = userIds.size();
            userRepository.deleteAllByIdInBatch(userIds);
        }

        log.info(
                "[LoadtestCleanup] runId={} targetUserCount={} deletedResumeCount={} deletedVersionCount={} deletedNotificationCount={} deletedRefreshTokenCount={} deletedPolicyAgreementCount={} deletedUserSettingCount={} deletedAuthCount={} deletedUserCount={}",
                runId,
                userIds.size(),
                deletedResumeCount,
                deletedVersionCount,
                deletedNotificationCount,
                deletedRefreshTokenCount,
                deletedPolicyAgreementCount,
                deletedUserSettingCount,
                deletedAuthCount,
                deletedUserCount);

        return new LoadtestCleanupResponse(
                runId,
                userIds.size(),
                deletedResumeCount,
                deletedVersionCount,
                deletedNotificationCount,
                deletedRefreshTokenCount,
                deletedPolicyAgreementCount,
                deletedUserSettingCount,
                deletedAuthCount,
                deletedUserCount);
    }

    public LoadtestCacheEvictResponse evictCache(LoadtestCacheEvictRequest request) {
        String cacheName = normalizeCacheName(request.cacheName());
        String key = normalizeCacheKey(request.key());

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        boolean existedBeforeEvict = cache.get(key) != null;
        cache.evict(key);

        log.info(
                "[LoadtestCacheEvict] cacheName={} key={} existedBeforeEvict={}",
                cacheName,
                key,
                existedBeforeEvict);

        return new LoadtestCacheEvictResponse(cacheName, key, existedBeforeEvict);
    }

    public JsonNode requestResumeGenerate(ResumeCreateRequest request) {
        String resumeGeneratePath = loadtestProperties.resolveResumeGeneratePath();
        if (!StringUtils.hasText(resumeGeneratePath)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String url = aiProperties.getBaseUrl() + resumeGeneratePath;

        try {
            JsonNode response =
                    aiClient.post().uri(url).body(request).retrieve().body(JsonNode.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[LoadtestResume] request failed url={} error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Transactional
    public LoadtestResumeBulkSeedResponse bulkSeedResumes(LoadtestResumeBulkSeedRequest request) {
        String runId = normalizeRunId(request.runId());
        int userCount = normalizeSeedUserCount(request.userCount());
        int startIndex = normalizePositiveStartIndex(request.startIndex());
        int resumesPerUser = normalizeResumeCount(request.resumesPerUser());
        int succeededVersionsPerResume =
                normalizeVersionCount(request.succeededVersionsPerResume());
        int failedVersionsPerResume = normalizeVersionCount(request.failedVersionsPerResume());
        int pendingVersionsPerResume = normalizeVersionCount(request.pendingVersionsPerResume());
        long pendingStartedMinutesAgo =
                normalizePendingStartedMinutesAgo(request.pendingStartedMinutesAgo());
        int totalVersionsPerResume =
                succeededVersionsPerResume + failedVersionsPerResume + pendingVersionsPerResume;

        if (totalVersionsPerResume <= 0 || totalVersionsPerResume > MAX_VERSIONS_PER_RESUME) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        Position position = resolveSeedPosition(request.positionId());
        boolean seedNotifications = Boolean.TRUE.equals(request.seedNotifications());
        List<User> users = resolveOrCreateActiveMockUsers(runId, userCount, startIndex);

        int createdResumeCount = 0;
        int createdVersionCount = 0;
        int createdNotificationCount = 0;

        for (int userOffset = 0; userOffset < users.size(); userOffset++) {
            User user = users.get(userOffset);
            int sequence = startIndex + userOffset;

            for (int resumeIndex = 1; resumeIndex <= resumesPerUser; resumeIndex++) {
                Resume resume =
                        resumeRepository.save(
                                Resume.create(
                                        user,
                                        position,
                                        null,
                                        buildResumeSeedName(sequence, resumeIndex)));

                int versionNo = 1;
                for (int count = 0; count < succeededVersionsPerResume; count++) {
                    ResumeVersion version =
                            createAndSaveResumeVersion(
                                    resume,
                                    versionNo,
                                    buildSeedResumeContentJson(
                                            runId, sequence, resumeIndex, versionNo, "success"));
                    version.succeed(
                            buildSeedResumeContentJson(
                                    runId, sequence, resumeIndex, versionNo, "success"));
                    createdVersionCount++;
                    versionNo++;
                }

                for (int count = 0; count < failedVersionsPerResume; count++) {
                    ResumeVersion version =
                            createAndSaveResumeVersion(
                                    resume,
                                    versionNo,
                                    buildSeedResumeContentJson(
                                            runId, sequence, resumeIndex, versionNo, "failed"));
                    version.failNow(
                            LOADTEST_SEED_ERROR_CODE,
                            "loadtest seeded failed version runId="
                                    + runId
                                    + " sequence="
                                    + sequence
                                    + " resumeIndex="
                                    + resumeIndex
                                    + " versionNo="
                                    + versionNo);
                    createdVersionCount++;
                    versionNo++;
                }

                for (int count = 0; count < pendingVersionsPerResume; count++) {
                    ResumeVersion version = createAndSaveResumeVersion(resume, versionNo, "{}");
                    version.startProcessing(
                            buildSeedAiTaskId(runId, sequence, resumeIndex, versionNo));
                    if (pendingStartedMinutesAgo > 0) {
                        version.overrideProcessingStartedAt(
                                Instant.now(clock)
                                        .minus(Duration.ofMinutes(pendingStartedMinutesAgo)));
                    }
                    createdVersionCount++;
                    versionNo++;
                }

                resume.setCurrentVersionNo(totalVersionsPerResume);
                createdResumeCount++;

                if (seedNotifications) {
                    notificationRepository.save(
                            Notification.create(
                                    user,
                                    NotificationType.RESUME,
                                    buildSeedNotificationPayload(
                                            runId, resume.getId(), sequence, resumeIndex)));
                    createdNotificationCount++;
                }
            }
        }

        log.info(
                "[LoadtestResumeSeed] runId={} processedUserCount={} createdResumeCount={} createdVersionCount={} createdNotificationCount={}",
                runId,
                users.size(),
                createdResumeCount,
                createdVersionCount,
                createdNotificationCount);

        return new LoadtestResumeBulkSeedResponse(
                runId,
                userCount,
                users.size(),
                createdResumeCount,
                createdVersionCount,
                createdNotificationCount);
    }

    @Transactional
    public LoadtestResumeCallbackReplayResponse replayResumeCallbacks(
            LoadtestResumeCallbackReplayRequest request) {
        int replayLimit = normalizeCallbackReplayLimit(request.limit());
        int duplicateCount = normalizeDuplicateCount(request.duplicateCount());
        LoadtestResumeReplayResultStatus resultStatus =
                normalizeReplayResultStatus(request.resultStatus());
        LoadtestResumeCallbackType callbackType = normalizeCallbackType(request.callbackType());
        String runId =
                request.runId() == null || request.runId().isBlank()
                        ? null
                        : normalizeRunId(request.runId());

        List<ResumeVersion> targetVersions = resolveReplayTargets(request, runId, replayLimit);
        int replayedCallbackCount = 0;

        for (ResumeVersion version : targetVersions) {
            for (int attempt = 0; attempt < duplicateCount; attempt++) {
                AiResumeCallbackRequest callbackRequest =
                        buildCallbackReplayRequest(version, resultStatus, attempt);

                if (callbackType == LoadtestResumeCallbackType.EDIT) {
                    resumeAiCallbackService.handleEditCallback(callbackRequest);
                } else {
                    resumeAiCallbackService.handleCallback(callbackRequest);
                }
                replayedCallbackCount++;
            }
        }

        log.info(
                "[LoadtestResumeReplay] runId={} targetVersionCount={} replayedCallbackCount={} duplicateCount={} resultStatus={} callbackType={}",
                runId,
                targetVersions.size(),
                replayedCallbackCount,
                duplicateCount,
                resultStatus,
                callbackType);

        return new LoadtestResumeCallbackReplayResponse(
                runId,
                targetVersions.size(),
                replayedCallbackCount,
                duplicateCount,
                resultStatus,
                callbackType);
    }

    @Transactional
    public LoadtestResumeResetResponse resetResumes(LoadtestResumeResetRequest request) {
        String runId = normalizeRunId(request.runId());
        boolean deleteNotifications =
                request.deleteNotifications() == null || request.deleteNotifications();
        List<Long> userIds = resolveMockUserIdsByRunId(runId);

        if (userIds.isEmpty()) {
            return new LoadtestResumeResetResponse(runId, 0, 0, 0, 0);
        }

        List<Resume> resumes = resumeRepository.findByUser_IdIn(userIds);
        List<Long> resumeIds = resumes.stream().map(Resume::getId).toList();
        int deletedVersionCount = 0;

        if (!resumeIds.isEmpty()) {
            deletedVersionCount = resumeVersionRepository.findAllByResume_IdIn(resumeIds).size();
            resumeVersionRepository.deleteByResume_IdIn(resumeIds);
            resumeRepository.deleteAllInBatch(resumes);
        }

        int deletedNotificationCount =
                deleteNotifications ? notificationRepository.deleteAllByUserIds(userIds) : 0;

        log.info(
                "[LoadtestResumeReset] runId={} targetUserCount={} deletedResumeCount={} deletedVersionCount={} deletedNotificationCount={}",
                runId,
                userIds.size(),
                resumes.size(),
                deletedVersionCount,
                deletedNotificationCount);

        return new LoadtestResumeResetResponse(
                runId,
                userIds.size(),
                resumes.size(),
                deletedVersionCount,
                deletedNotificationCount);
    }

    private List<User> resolveOrCreateActiveMockUsers(String runId, int count, int startIndex) {
        List<User> users = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int sequence = startIndex + i;
            MockIdentity identity = buildIdentity(runId, "u" + sequence);
            Auth auth =
                    authRepository
                            .findByProviderAndProviderUserId(
                                    MOCK_PROVIDER, identity.providerUserId())
                            .orElseGet(() -> createMockUser(identity, UserStatus.ACTIVE).auth());

            ensureMockAuth(auth);
            if (auth.getUser().getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            users.add(auth.getUser());
        }

        return users;
    }

    private List<Long> resolveMockUserIdsByRunId(String runId) {
        return authRepository
                .findAllByProviderAndProviderUserIdStartingWith(
                        MOCK_PROVIDER, buildProviderUserIdPrefix(runId))
                .stream()
                .map(auth -> auth.getUser().getId())
                .distinct()
                .toList();
    }

    private Position resolveSeedPosition(Long positionId) {
        if (positionId == null || positionId <= 0) {
            return resolveDefaultPositionForActiveMock();
        }

        return positionRepository
                .findById(positionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    }

    private ResumeVersion createAndSaveResumeVersion(Resume resume, int versionNo, String content) {
        ResumeVersion version =
                versionNo == 1
                        ? ResumeVersion.createV1(resume, content)
                        : ResumeVersion.createNext(resume, versionNo, content);
        return resumeVersionRepository.save(version);
    }

    private String buildResumeSeedName(int userSequence, int resumeIndex) {
        String candidate = "lt-rs-" + userSequence + "-" + resumeIndex;
        return candidate.length() <= 30 ? candidate : candidate.substring(0, 30);
    }

    private String buildSeedAiTaskId(String runId, int sequence, int resumeIndex, int versionNo) {
        String seed = runId + "-" + sequence + "-" + resumeIndex + "-" + versionNo;
        String normalized = seed.replaceAll("[^A-Za-z0-9-]", "");
        String truncated =
                normalized.length() > 20
                        ? normalized.substring(normalized.length() - 20)
                        : normalized;
        return "lt-" + truncated + "-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private String buildSeedResumeContentJson(
            String runId, int sequence, int resumeIndex, int versionNo, String stage) {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "runId", runId,
                            "sequence", sequence,
                            "resumeIndex", resumeIndex,
                            "versionNo", versionNo,
                            "stage", stage,
                            "techStack", List.of("Java", "Spring", "k6")));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private String buildSeedNotificationPayload(
            String runId, Long resumeId, int sequence, int resumeIndex) {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "runId", runId,
                            "resumeId", resumeId,
                            "sequence", sequence,
                            "resumeIndex", resumeIndex,
                            "message", "loadtest seeded notification"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private List<ResumeVersion> resolveReplayTargets(
            LoadtestResumeCallbackReplayRequest request, String runId, int replayLimit) {
        if (request.versionIds() != null && !request.versionIds().isEmpty()) {
            return resumeVersionRepository.findAllById(request.versionIds()).stream()
                    .filter(version -> StringUtils.hasText(version.getAiTaskId()))
                    .toList();
        }

        if (!StringUtils.hasText(runId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        List<Long> userIds = resolveMockUserIdsByRunId(runId);
        if (userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return resumeVersionRepository.findByResume_User_IdInAndStatusOrderByIdAsc(
                userIds, ResumeVersionStatus.PROCESSING, PageRequest.of(0, replayLimit));
    }

    private AiResumeCallbackRequest buildCallbackReplayRequest(
            ResumeVersion version, LoadtestResumeReplayResultStatus resultStatus, int attempt) {
        if (!StringUtils.hasText(version.getAiTaskId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (resultStatus == LoadtestResumeReplayResultStatus.FAILED) {
            return new AiResumeCallbackRequest(
                    version.getAiTaskId(),
                    "failed",
                    null,
                    new AiResumeCallbackRequest.ErrorPayload(
                            "LOADTEST_REPLAY_FAILED",
                            "loadtest replay failed callback attempt=" + attempt));
        }

        return new AiResumeCallbackRequest(
                version.getAiTaskId(),
                "success",
                new AiResumeCallbackRequest.ResumePayload(
                        List.of("Java", "Spring", "k6"),
                        List.of(
                                new AiResumeCallbackRequest.ProjectPayload(
                                        "loadtest-project-" + version.getId(),
                                        "https://github.com/openai/openai-openapi",
                                        "loadtest replay callback payload attempt=" + attempt,
                                        List.of("Java", "Spring")))),
                null);
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
        Long defaultPositionId = loadtestProperties.getMockAuth().getDefaultPositionId();
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
                        .agreedAt(Instant.now(clock))
                        .build());
    }

    private SessionTokens issueSessionTokens(User user) {
        AuthTokenReissueResult issued =
                authSessionIssueService.issueTokens(user.getId(), user.getStatus());
        return new SessionTokens(issued.accessToken(), issued.refreshToken());
    }

    private int revokeAllRefreshTokens(Long userId) {
        return revokeAllRefreshTokens(userId, Instant.now(clock));
    }

    private int revokeAllRefreshTokens(Long userId, Instant revokedAt) {
        List<String> activeTokenHashes =
                refreshTokenRepository.findActiveTokenHashesByUserId(userId);
        if (activeTokenHashes.isEmpty()) {
            return 0;
        }

        refreshTokenRepository.revokeAllByUserId(userId, revokedAt);
        refreshTokenCacheService.evictAll(activeTokenHashes);
        return activeTokenHashes.size();
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
        int maxBulkCount = Math.max(1, loadtestProperties.getMockAuth().getMaxBulkCount());
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

    private int normalizeSeedUserCount(Integer count) {
        if (count == null || count <= 0 || count > MAX_SEED_USER_COUNT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return count;
    }

    private int normalizePositiveStartIndex(Integer startIndex) {
        int normalized = startIndex == null ? 1 : startIndex;
        if (normalized <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private int normalizeResumeCount(Integer count) {
        if (count == null || count <= 0 || count > MAX_RESUMES_PER_USER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return count;
    }

    private int normalizeVersionCount(Integer count) {
        if (count == null) {
            return 0;
        }
        if (count < 0 || count > MAX_VERSIONS_PER_RESUME) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return count;
    }

    private int normalizeCallbackReplayLimit(Integer limit) {
        if (limit == null) {
            return 100;
        }
        if (limit <= 0 || limit > MAX_CALLBACK_REPLAY_LIMIT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return limit;
    }

    private int normalizeDuplicateCount(Integer duplicateCount) {
        if (duplicateCount == null) {
            return 1;
        }
        if (duplicateCount <= 0 || duplicateCount > MAX_CALLBACK_DUPLICATE_COUNT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return duplicateCount;
    }

    private long normalizePendingStartedMinutesAgo(Long minutesAgo) {
        if (minutesAgo == null) {
            return 0L;
        }
        if (minutesAgo < 0 || minutesAgo > 1_440) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return minutesAgo;
    }

    private LoadtestResumeReplayResultStatus normalizeReplayResultStatus(
            LoadtestResumeReplayResultStatus resultStatus) {
        return resultStatus == null ? LoadtestResumeReplayResultStatus.SUCCESS : resultStatus;
    }

    private LoadtestResumeCallbackType normalizeCallbackType(
            LoadtestResumeCallbackType callbackType) {
        return callbackType == null ? LoadtestResumeCallbackType.CREATE : callbackType;
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

    private String normalizeCacheName(String cacheName) {
        if (!StringUtils.hasText(cacheName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        String normalized = cacheName.trim();
        if (!SUPPORTED_CACHE_NAME_POSITIONS.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCacheKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return key.trim();
    }

    private String normalizeRunId(String runId) {
        return normalizeIdentifier(runId);
    }

    private String normalizeUserKey(String userKey) {
        return normalizeIdentifier(userKey);
    }

    private String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
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
