package com.sipomeokjo.commitme.domain.user.dto;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;

public record UserUpdateResponse(
        Long id,
        String profileImageUrl,
        String name,
        Long positionId,
        String phone,
        UserStatus status) {}
