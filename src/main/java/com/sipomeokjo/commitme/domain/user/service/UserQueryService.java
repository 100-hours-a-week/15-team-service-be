package com.sipomeokjo.commitme.domain.user.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyAgreement;
import com.sipomeokjo.commitme.domain.policy.entity.PolicyType;
import com.sipomeokjo.commitme.domain.policy.repository.PolicyAgreementRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.dto.UserProfileResponse;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.mapper.UserMapper;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import java.util.List;
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
    private final PolicyAgreementRepository policyAgreementRepository;

    public UserProfileResponse getUserProfile(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PolicyAgreementStatus policyAgreementStatus = resolvePolicyAgreementStatus(userId);
        return userMapper.toProfileResponse(
                user,
                s3UploadService.toCdnUrl(user.getProfileImageUrl()),
                policyAgreementStatus.privacyAgreed(),
                policyAgreementStatus.phonePolicyAgreed());
    }

    private PolicyAgreementStatus resolvePolicyAgreementStatus(Long userId) {
        List<PolicyAgreement> agreements = policyAgreementRepository.findAllByUser_Id(userId);
        boolean privacyAgreed =
                agreements.stream()
                        .anyMatch(agreement -> agreement.getPolicyType() == PolicyType.PRIVACY);
        boolean phonePolicyAgreed =
                agreements.stream()
                        .anyMatch(
                                agreement -> agreement.getPolicyType() == PolicyType.PHONE_PRIVACY);
        return new PolicyAgreementStatus(privacyAgreed, phonePolicyAgreed);
    }

    private record PolicyAgreementStatus(boolean privacyAgreed, boolean phonePolicyAgreed) {}
}
