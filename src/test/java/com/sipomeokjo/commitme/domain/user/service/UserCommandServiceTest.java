package com.sipomeokjo.commitme.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.mapper.UserValidationExceptionMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserFinder userFinder;
    @Mock private PositionFinder positionFinder;
    @Mock private PolicyAgreementRepository policyAgreementRepository;
    @Mock private UserMapper userMapper;
    @Mock private UserValidationExceptionMapper userValidationExceptionMapper;
    @Mock private S3UploadService s3UploadService;
    @Mock private AuthRepository authRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    private UserCommandService userCommandService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
        userCommandService =
                new UserCommandService(
                        clock,
                        authRepository,
                        refreshTokenRepository,
                        userRepository,
                        policyAgreementRepository,
                        s3UploadService,
                        userFinder,
                        positionFinder,
                        userMapper,
                        userValidationExceptionMapper);
    }

    @Test
    void updateProfile_phonePolicyFalseOnly_updatesWithoutRequiredFieldValidation() {
        Long userId = 1L;
        User user =
                User.builder()
                        .id(userId)
                        .name("홍길동")
                        .phone("01012345678")
                        .profileImageUrl("old-profile")
                        .status(UserStatus.ACTIVE)
                        .build();
        UserUpdateRequest request = new UserUpdateRequest(null, null, null, null, null, false);
        UserUpdateResponse expected =
                new UserUpdateResponse(
                        userId, "cdn-profile", "홍길동", null, "01012345678", UserStatus.ACTIVE);

        given(userFinder.getByIdOrThrow(userId)).willReturn(user);
        given(s3UploadService.toCdnUrl("old-profile")).willReturn("cdn-profile");
        given(userMapper.toUpdateResponse(user, "cdn-profile")).willReturn(expected);

        UserUpdateResponse actual = userCommandService.updateProfile(userId, request);

        assertThat(actual).isEqualTo(expected);
        verify(policyAgreementRepository)
                .deleteAllByUser_IdAndPolicyType(userId, PolicyType.PHONE_PRIVACY);
        verifyNoInteractions(positionFinder);
    }
}
