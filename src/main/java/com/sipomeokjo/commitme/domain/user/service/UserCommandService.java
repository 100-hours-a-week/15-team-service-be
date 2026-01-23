package com.sipomeokjo.commitme.domain.user.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingRequest;
import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateRequest;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
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

	public OnboardingResponse onboard(Long userId, OnboardingRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST));

		if (user.getStatus() == UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.USER_ALREADY_ONBOARDED);
		}

		validateName(request.name());
		Position position = resolvePosition(request.positionId());
		validatePrivacyAgreement(request.privacyAgreed());
		validatePhonePolicy(request.phone(), request.phonePolicyAgreed());
		validatePhoneFormat(request.phone());

		user.updateOnboarding(
				position,
				request.name().trim(),
				request.phone(),
				request.profileImageUrl(),
				UserStatus.ACTIVE
		);

		savePrivacyAgreements(user, request.phone());

		return userMapper.toOnboardingResponse(user);
	}

	public UserUpdateResponse updateProfile(Long userId, UserUpdateRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validateUpdateName(request.name());
		Position position = resolveUpdatePosition(request.positionId());
		validateUpdatePrivacyAgreement(request.privacyAgreed());
		validateUpdatePhonePolicy(request.phone(), request.phonePolicyAgreed());
		validateUpdatePhoneLength(request.phone());

		String nextPhone = request.phone() == null ? user.getPhone() : request.phone();
		String nextProfileImageUrl = request.profileImageUrl() == null ? user.getProfileImageUrl() : request.profileImageUrl();

		user.updateProfile(
				position,
				request.name().trim(),
				nextPhone,
				nextProfileImageUrl
		);

		savePrivacyAgreements(user, request.phone());

		return userMapper.toUpdateResponse(user);
	}

	private void validateName(String name) {
		if (name == null) {
			throw new BusinessException(ErrorCode.NAME_INVALID_INPUT);
		}
		String trimmed = name.replaceAll("\\s+", "");
		if (trimmed.length() < 2) {
			throw new BusinessException(ErrorCode.NAME_INVALID_INPUT);
		}
	}

	private Position resolvePosition(Long positionId) {
		if (positionId == null) {
			throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
		}
		return positionRepository.findById(positionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED));
	}

	private void validatePrivacyAgreement(Boolean privacyAgreed) {
		if (privacyAgreed == null || !privacyAgreed) {
			throw new BusinessException(ErrorCode.USER_PRIVACY_REQUIRED);
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

	private void validatePhoneFormat(String phone) {
		if (phone == null || phone.isBlank()) {
			return;
		}
		if (!phone.matches("^[0-9]+$")) {
			throw new BusinessException(ErrorCode.BAD_REQUEST);
		}
	}

	private void savePrivacyAgreements(User user, String phone) {
		policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PRIVACY));

		if (phone == null || phone.isBlank()) {
			return;
		}

		policyAgreementRepository.save(buildPolicyAgreement(user, PolicyType.PHONE_PRIVACY));
	}

	private void validateUpdateName(String name) {
		if (name == null || name.isBlank()) {
			throw new BusinessException(ErrorCode.USER_NAME_REQUIRED);
		}
		String trimmed = name.trim();
		if (trimmed.length() < 2 || trimmed.length() > 10) {
			throw new BusinessException(ErrorCode.USER_NAME_LENGTH_OUT_OF_RANGE);
		}
	}

	private Position resolveUpdatePosition(Long positionId) {
		if (positionId == null) {
			throw new BusinessException(ErrorCode.USER_POSITION_REQUIRED);
		}
		if (positionId <= 0) {
			throw new BusinessException(ErrorCode.USER_POSITION_INVALID);
		}
		return positionRepository.findById(positionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
	}

	private void validateUpdatePrivacyAgreement(Boolean privacyAgreed) {
		if (privacyAgreed == null) {
			throw new BusinessException(ErrorCode.USER_POLICY_AGREED_REQUIRED);
		}
		if (!privacyAgreed) {
			throw new BusinessException(ErrorCode.USER_POLICY_AGREED_MUST_BE_TRUE);
		}
	}

	private void validateUpdatePhonePolicy(String phone, Boolean phonePolicyAgreed) {
		if (phone == null || phone.isBlank()) {
			return;
		}
		if (phonePolicyAgreed == null || !phonePolicyAgreed) {
			throw new BusinessException(ErrorCode.USER_PHONE_POLICY_AGREED_REQUIRED);
		}
	}

	private void validateUpdatePhoneLength(String phone) {
		if (phone == null || phone.isBlank()) {
			return;
		}
		if (phone.length() < 11 || phone.length() > 20) {
			throw new BusinessException(ErrorCode.USER_PHONE_LENGTH_OUT_OF_RANGE);
		}
	}

	private PolicyAgreement buildPolicyAgreement(User user, PolicyType policyType) {
		return PolicyAgreement.builder()
				.user(user)
				.document("dummy")
				.policyType(policyType)
				.policyVersion("0000-00-00")
				.agreedAt(LocalDateTime.now())
				.build();
	}
}
