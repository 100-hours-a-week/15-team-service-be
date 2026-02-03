package com.sipomeokjo.commitme.domain.user.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.dto.UserProfileResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final S3UploadService s3UploadService;

    public UserProfileResponse getUserProfile(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String profileImageUrl = s3UploadService.toCdnUrl(user.getProfileImageUrl());
        return userMapper.toProfileResponse(user, profileImageUrl);
    }
}
