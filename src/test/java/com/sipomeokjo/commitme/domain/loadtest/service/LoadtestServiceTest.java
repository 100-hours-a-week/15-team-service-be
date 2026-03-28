package com.sipomeokjo.commitme.domain.loadtest.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.config.LoadtestProperties;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.auth.service.AuthSessionIssueService;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeBulkSeedRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeForceCompleteRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeForceEditRequest;
import com.sipomeokjo.commitme.domain.loadtest.resume.dto.LoadtestResumeReplayResultStatus;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionCacheRepository;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiCallbackService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeLockService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProjectionService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.ResumeProfileRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.userSetting.repository.UserSettingRepository;
import com.sipomeokjo.commitme.global.mongo.MongoSequenceService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class LoadtestServiceTest {

    @Mock private AuthRepository authRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserSettingRepository userSettingRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private PolicyAgreementRepository policyAgreementRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenCacheService refreshTokenCacheService;
    @Mock private AuthSessionIssueService authSessionIssueService;
    @Mock private ResumeMongoRepository resumeMongoRepository;
    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private MongoSequenceService mongoSequenceService;
    @Mock private ResumeProjectionService resumeProjectionService;
    @Mock private ResumeLockService resumeLockService;
    @Mock private ResumeProfileRepository resumeProfileRepository;
    @Mock private ResumeAiCallbackService resumeAiCallbackService;
    @Mock private NotificationRepository notificationRepository;
    @Mock private LoadtestProperties loadtestProperties;
    @Mock private PositionCacheRepository positionCacheRepository;
    @Mock private RestClient aiClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private AiProperties aiProperties;
    @Mock private Clock clock;
    @Mock private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks private LoadtestService loadtestService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(clock.instant()).thenReturn(Instant.parse("2026-03-28T00:00:00Z"));
        Mockito.lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Mockito.lenient()
                .when(resumeMongoRepository.save(any(ResumeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient()
                .when(resumeEventMongoRepository.save(any(ResumeEventDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bulkSeedResumes_whenFailedOnly_clearsPendingProjection() {
        Position position = mockPosition();
        Auth auth = mockActiveMockAuth(11L);

        given(positionRepository.findById(1L)).willReturn(Optional.of(position));
        given(authRepository.findByProviderAndProviderUserId(eq(AuthProvider.GITHUB), anyString()))
                .willReturn(Optional.of(auth));
        given(mongoSequenceService.nextResumeId()).willReturn(100L);

        loadtestService.bulkSeedResumes(
                new LoadtestResumeBulkSeedRequest("run-a", 1, 1, 1, 0, 1, 0, 0L, 1L, false));

        verify(resumeProjectionService).applyAiFailure(100L, 1);
        verify(resumeProjectionService, never()).setPendingWorkStarted(100L);
    }

    @Test
    void bulkSeedResumes_whenSucceededAndPending_marksPendingAfterCommittedSuccess() {
        Position position = mockPosition();
        Auth auth = mockActiveMockAuth(11L);

        given(positionRepository.findById(1L)).willReturn(Optional.of(position));
        given(authRepository.findByProviderAndProviderUserId(eq(AuthProvider.GITHUB), anyString()))
                .willReturn(Optional.of(auth));
        given(mongoSequenceService.nextResumeId()).willReturn(100L);

        loadtestService.bulkSeedResumes(
                new LoadtestResumeBulkSeedRequest("run-b", 1, 1, 1, 1, 0, 1, 0L, 1L, false));

        verify(resumeProjectionService).applyAiSuccess(100L, 1, true);
        verify(resumeProjectionService).applyVersionCommitted(100L, 1);
        verify(resumeProjectionService).setPendingWorkStarted(100L);
        verify(resumeProjectionService, never()).applyAiFailure(100L, 2);
    }

    @Test
    void forceCompleteResumes_whenFailedActualCreate_releasesCreateLock() {
        ResumeEventDocument doc =
                ResumeEventDocument.create(100L, 1, 11L, ResumeVersionStatus.QUEUED, "{}");
        doc.startProcessing("real-job", Instant.parse("2026-03-28T00:00:00Z"));

        given(
                        resumeEventMongoRepository.findByResumeIdInAndStatusIn(
                                eq(List.of(100L)), anyList(), eq(PageRequest.of(0, 10))))
                .willReturn(List.of(doc));

        loadtestService.forceCompleteResumes(
                new LoadtestResumeForceCompleteRequest(
                        null, List.of(100L), 10, LoadtestResumeReplayResultStatus.FAILED));

        verify(resumeProjectionService).applyAiFailure(100L, 1);
        verify(resumeLockService).releaseCreateLock(11L, 100L);
    }

    @Test
    void forceCompleteResumes_whenSeedPending_skipsLockRelease() {
        ResumeEventDocument doc =
                ResumeEventDocument.create(100L, 2, 11L, ResumeVersionStatus.QUEUED, "{}");
        doc.startProcessing("lt-seed-job", Instant.parse("2026-03-28T00:00:00Z"));

        given(
                        resumeEventMongoRepository.findByResumeIdInAndStatusIn(
                                eq(List.of(100L)), anyList(), eq(PageRequest.of(0, 10))))
                .willReturn(List.of(doc));

        loadtestService.forceCompleteResumes(
                new LoadtestResumeForceCompleteRequest(
                        null, List.of(100L), 10, LoadtestResumeReplayResultStatus.SUCCESS));

        verify(resumeProjectionService).applyAiSuccess(100L, 2, false);
        verify(resumeProjectionService).applyVersionCommitted(100L, 2);
        verify(resumeLockService, never()).releaseCreateLock(any(), any());
        verify(resumeLockService, never()).releaseEditLock(any(), any());
    }

    @Test
    void forceEdit_whenIsPendingTrue_throwsConflict() {
        given(resumeEventMongoRepository.existsByResumeIdAndIsPendingTrue(100L)).willReturn(true);

        assertThatThrownBy(
                        () ->
                                loadtestService.forceEdit(
                                        new LoadtestResumeForceEditRequest(11L, 100L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESUME_EDIT_IN_PROGRESS);

        verify(resumeProjectionService).findDocumentByResumeIdAndUserIdOrThrow(100L, 11L);
    }

    private Position mockPosition() {
        Position position = org.mockito.Mockito.mock(Position.class);
        given(position.getId()).willReturn(1L);
        given(position.getName()).willReturn("Backend");
        return position;
    }

    private Auth mockActiveMockAuth(Long userId) {
        User user = org.mockito.Mockito.mock(User.class);
        Auth auth = org.mockito.Mockito.mock(Auth.class);
        given(user.getId()).willReturn(userId);
        given(user.getStatus()).willReturn(UserStatus.ACTIVE);
        given(auth.getUser()).willReturn(user);
        given(auth.getProviderUserId()).willReturn("lt:run:user");
        return auth;
    }
}
