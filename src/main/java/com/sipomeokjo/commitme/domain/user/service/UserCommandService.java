package com.sipomeokjo.commitme.domain.user.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.Auth;
import com.sipomeokjo.commitme.domain.auth.repository.AuthRepository;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
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

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final PolicyAgreementRepository policyAgreementRepository;
    private final UserMapper userMapper;
    private final S3UploadService s3UploadService;
    private final AuthRepository authRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheService refreshTokenCacheService;
    private final Clock clock;

    public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST));

        validateRejoin(user);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ONBOARDED);
        }

        validateName(request.name());
        Position position = resolvePosition(request.positionId());
        validatePrivacyAgreement(request.privacyAgreed());
        validatePhonePolicy(request.phone(), request.phonePolicyAgreed());
        validatePhone(request.phone());

        user.updateOnboarding(
                position,
                request.name().trim(),
                request.phone(),
                s3UploadService.toS3Key(request.profileImageUrl()),
                UserStatus.ACTIVE);

        savePrivacyAgreements(user, request.phone());

        return userMapper.toOnboardingResponse(user);
    }

    public UserUpdateResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateName(request.name());
        Position position = resolvePosition(request.positionId());
        validatePrivacyAgreement(request.privacyAgreed());
        validatePhonePolicy(request.phone(), request.phonePolicyAgreed());
        validatePhone(request.phone());

        String nextPhone = request.phone();
        String nextProfileImageUrl = s3UploadService.toS3Key(request.profileImageUrl());

        user.updateProfile(position, request.name().trim(), nextPhone, nextProfileImageUrl);

        savePrivacyAgreements(user, nextPhone);

        String profileImageUrl = s3UploadService.toCdnUrl(user.getProfileImageUrl());
        return userMapper.toUpdateResponse(user, profileImageUrl);
    }

    public void deactivate(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.deactivate(Instant.now(clock));

        var activeTokenHashes = refreshTokenRepository.findActiveTokenHashesByUserId(userId);
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now(clock));
        refreshTokenCacheService.evictAll(activeTokenHashes);

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

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.USER_NAME_REQUIRED);
        }
        if (containsWhitespace(name) || containsEmoji(name)) {
            throw new BusinessException(ErrorCode.USER_NAME_INVALID);
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 10) {
            throw new BusinessException(ErrorCode.USER_NAME_LENGTH_OUT_OF_RANGE);
        }
    }

    private void validateRejoin(User user) {
        if (user == null || user.getStatus() != UserStatus.INACTIVE) {
            return;
        }
        Instant deletedAt = user.getDeletedAt();
        if (deletedAt == null) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_WITHDRAWN);
        }
        Instant now = Instant.now(clock);
        Instant rejoinAvailableAt = deletedAt.plus(Duration.ofDays(30));
        if (now.isBefore(rejoinAvailableAt)) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_WITHDRAWN);
        }
        user.restoreForRejoin();
    }

    private Position resolvePosition(Long positionId) {
        if (positionId == null) {
            throw new BusinessException(ErrorCode.USER_POSITION_REQUIRED);
        }
        if (positionId <= 0) {
            throw new BusinessException(ErrorCode.USER_POSITION_INVALID);
        }
        return positionRepository
                .findById(positionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    }

    private void validatePrivacyAgreement(Boolean privacyAgreed) {
        if (privacyAgreed == null) {
            throw new BusinessException(ErrorCode.USER_POLICY_AGREED_REQUIRED);
        }
        if (!privacyAgreed) {
            throw new BusinessException(ErrorCode.USER_POLICY_AGREED_MUST_BE_TRUE);
        }
    }

    private void validatePhone(String phone) {
        if (phone == null) {
            return;
        }
        if (phone.isBlank()) {
            throw new BusinessException(ErrorCode.USER_PHONE_LENGTH_OUT_OF_RANGE);
        }
        if (phone.length() < 11 || phone.length() > 20) {
            throw new BusinessException(ErrorCode.USER_PHONE_LENGTH_OUT_OF_RANGE);
        }
        if (!phone.matches("^[0-9]+$")) {
            throw new BusinessException(ErrorCode.USER_PHONE_INVALID);
        }
    }

    private void savePrivacyAgreements(User user, String phone) {
        policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PRIVACY));

        if (phone == null || phone.isBlank()) {
            return;
        }

        policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PHONE_PRIVACY));
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

    private boolean containsWhitespace(String value) {
        return value.codePoints().anyMatch(Character::isWhitespace);
    }

    private boolean containsEmoji(String value) {
        return value.codePoints().anyMatch(UserCommandService::isEmojiCodePoint);
    }

    private static boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F)
                || (codePoint >= 0x1F300 && codePoint <= 0x1F5FF)
                || (codePoint >= 0x1F680 && codePoint <= 0x1F6FF)
                || (codePoint >= 0x1F900 && codePoint <= 0x1F9FF)
                || (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x26FF)
                || (codePoint >= 0x2700 && codePoint <= 0x27BF)
                || (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF)
                || (codePoint >= 0xFE00 && codePoint <= 0xFE0F);
    }
}
