package com.sipomeokjo.commitme.domain.user.dto;

public record UserUpdateRequest(
        String profileImageUrl,
        String name,
        Long positionId,
        String phone,
        Boolean privacyAgreed,
        Boolean phonePolicyAgreed) {}
