package com.sipomeokjo.commitme.domain.user.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record OnboardingResponse(
		Long userId,
		UserStatus status
) {
}
