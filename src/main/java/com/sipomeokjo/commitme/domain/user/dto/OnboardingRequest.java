package com.sipomeokjo.commitme.domain.user.dto;

public record OnboardingRequest(
		String profileImageUrl,
		String name,
		Long positionId,
		String phone,
		Boolean privacyAgreed,
		Boolean phonePolicyAgreed
) {
}
