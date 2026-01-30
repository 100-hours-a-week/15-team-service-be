package com.sipomeokjo.commitme.domain.user.mapper;

import com.sipomeokjo.commitme.domain.user.dto.OnboardingResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserProfileResponse;
import com.sipomeokjo.commitme.domain.user.dto.UserUpdateResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user, String profileImageUrl) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                profileImageUrl,
                user.getName(),
                user.getPosition() == null ? null : user.getPosition().getId(),
                user.getPhone());
    }

    public UserUpdateResponse toUpdateResponse(User user, String profileImageUrl) {
        if (user == null) {
            return null;
        }
        return new UserUpdateResponse(
                user.getId(),
                profileImageUrl,
                user.getName(),
                user.getPosition() == null ? null : user.getPosition().getId(),
                user.getPhone(),
                user.getStatus());
    }

    public OnboardingResponse toOnboardingResponse(User user) {
        if (user == null) {
            return null;
        }
        return new OnboardingResponse(user.getId(), user.getStatus());
    }
}
