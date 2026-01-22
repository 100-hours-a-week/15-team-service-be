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
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserCommandService {

	private final UserRepository userRepository;
	private final PositionRepository positionRepository;
	private final PolicyAgreementRepository policyAgreementRepository;

	public UserCommandService(UserRepository userRepository,
							  PositionRepository positionRepository,
							  PolicyAgreementRepository policyAgreementRepository) {
		this.userRepository = userRepository;
		this.positionRepository = positionRepository;
		this.policyAgreementRepository = policyAgreementRepository;
	}

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

		return new OnboardingResponse(user.getId(), user.getStatus());
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
		policyAgreementRepository.save(PolicyAgreement.builder()
				.user(user)
				.document("dummy")
				.policyType(PolicyType.PRIVACY)
				.policyVersion("0000-00-00")
				.agreedAt(LocalDateTime.now())
				.build());

		if (phone == null || phone.isBlank()) {
			return;
		}

		policyAgreementRepository.save(PolicyAgreement.builder()
				.user(user)
				.document("dummy")
				.policyType(PolicyType.PHONE_PRIVACY)
				.policyVersion("0000-00-00")
				.agreedAt(LocalDateTime.now())
				.build());
	}
}
