package com.sipomeokjo.commitme.domain.user.dto;

public record UserProfileResponse(
        Long id,
        String profileImageUrl,
        String name,
        Long positionId,
        String phone
) {
}
