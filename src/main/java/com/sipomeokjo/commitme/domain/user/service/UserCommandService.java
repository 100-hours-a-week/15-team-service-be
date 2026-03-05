package com.sipomeokjo.commitme.domain.user.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserProfileValidationException;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.mapper.UserValidationExceptionMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandService {

    private final Clock clock;
    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PolicyAgreementRepository policyAgreementRepository;
    private final S3UploadService s3UploadService;
    private final UserFinder userFinder;
    private final PositionFinder positionFinder;
    private final UserMapper userMapper;
    private final UserValidationExceptionMapper validationExceptionMapper;

    public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST));

        validateOnboardingTarget(user);

        Position position = resolvePosition(request.positionId());
        validatePrivacyAgreement(request.privacyAgreed());
        validatePhonePolicy(request.phone(), request.phonePolicyAgreed());
        applyOnboarding(user, request, position);

        savePrivacyAgreements(user, request.phone());

        return userMapper.toOnboardingResponse(user);
    }

    public UserUpdateResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userFinder.getByIdOrThrow(userId);

        Position nextPosition = user.getPosition();
        String nextName = user.getName();
        String nextPhone = user.getPhone();
        String nextProfileImageUrl = user.getProfileImageUrl();

        if (request.positionId() != null) {
            nextPosition = resolvePosition(request.positionId());
        }
        if (request.name() != null) {
            nextName = request.name();
        }
        if (request.phone() != null) {
            nextPhone = request.phone();
        }
        if (request.profileImageUrl() != null) {
            nextProfileImageUrl = s3UploadService.toS3Key(request.profileImageUrl());
        }

        applyProfileUpdate(user, nextPosition, nextName, nextPhone, nextProfileImageUrl);

        updatePolicyAgreements(user, request.privacyAgreed(), request.phonePolicyAgreed());

        String profileImageUrl = s3UploadService.toCdnUrl(user.getProfileImageUrl());
        return userMapper.toUpdateResponse(user, profileImageUrl);
    }

    public void deactivate(Long userId) {
        User user = userFinder.getByIdOrThrow(userId);
        user.deactivate(Instant.now(clock));
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(clock));

        for (Auth auth : authRepository.findAllByUser_Id(userId)) {
            auth.clearSensitiveInfo();
        }
    }

    private void validatePhonePolicy(String phone, Boolean phonePolicyAgreed) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        if (phonePolicyAgreed == null || !phonePolicyAgreed) {
            throw new BusinessException(ErrorCode.USER_PHONE_PRIVACY_REQUIRED);
        }
    }

    private void validateOnboardingTarget(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ONBOARDED);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_WITHDRAWN);
        }
    }

    private Position resolvePosition(Long positionId) {
        if (positionId == null) {
            throw new BusinessException(ErrorCode.USER_POSITION_REQUIRED);
        }
        if (positionId <= 0) {
            throw new BusinessException(ErrorCode.USER_POSITION_INVALID);
        }
        return positionFinder.getByIdOrThrow(positionId);
    }

    private void validatePrivacyAgreement(Boolean privacyAgreed) {
        if (privacyAgreed == null) {
            throw new BusinessException(ErrorCode.USER_POLICY_AGREED_REQUIRED);
        }
        if (!privacyAgreed) {
            throw new BusinessException(ErrorCode.USER_POLICY_AGREED_MUST_BE_TRUE);
        }
    }

    private void savePrivacyAgreements(User user, String phone) {
        policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PRIVACY));

        if (phone == null || phone.isBlank()) {
            return;
        }

        policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PHONE_PRIVACY));
    }

    private void updatePolicyAgreements(
            User user, Boolean privacyAgreed, Boolean phonePolicyAgreed) {
        if (Boolean.TRUE.equals(privacyAgreed)) {
            policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PRIVACY));
        }

        if (phonePolicyAgreed == null) {
            return;
        }

        if (phonePolicyAgreed) {
            policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PHONE_PRIVACY));
            return;
        }

        policyAgreementRepository.deleteAllByUser_IdAndPolicyType(
                user.getId(), PolicyType.PHONE_PRIVACY);
    }

    private PolicyAgreement buildPolicyAgreement(User user, PolicyType policyType) {
        return PolicyAgreement.builder()
                .user(user)
                .document("dummy")
                .policyType(policyType)
                .policyVersion("0000-00-00")
                .agreedAt(Instant.now(clock))
                .build();
    }

    private void applyOnboarding(User user, OnboardingRequest request, Position position) {
        try {
            user.updateOnboarding(
                    position,
                    request.name(),
                    request.phone(),
                    s3UploadService.toS3Key(request.profileImageUrl()),
                    UserStatus.ACTIVE);
        } catch (UserProfileValidationException ex) {
            throw validationExceptionMapper.toBusinessException(ex);
        }
    }

    private void applyProfileUpdate(
            User user, Position position, String name, String phone, String profileImageUrl) {
        try {
            user.updateProfile(position, name, phone, profileImageUrl);
        } catch (UserProfileValidationException ex) {
            throw validationExceptionMapper.toBusinessException(ex);
        }
    }
}
