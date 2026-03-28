package com.sipomeokjo.commitme.domain.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileCreateResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileUpdateResponse;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeProfileMapper;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.entity.EducationStatus;
import com.sipomeokjo.commitme.domain.user.entity.EducationType;
import com.sipomeokjo.commitme.domain.user.entity.EmploymentType;
import com.sipomeokjo.commitme.domain.user.entity.EnumParseException;
import com.sipomeokjo.commitme.domain.user.entity.ResumeProfile;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserActivity;
import com.sipomeokjo.commitme.domain.user.entity.UserCertificate;
import com.sipomeokjo.commitme.domain.user.entity.UserEducation;
import com.sipomeokjo.commitme.domain.user.entity.UserExperience;
import com.sipomeokjo.commitme.domain.user.entity.UserProfileValidationException;
import com.sipomeokjo.commitme.domain.user.entity.UserTechStack;
import com.sipomeokjo.commitme.domain.user.mapper.UserValidationExceptionMapper;
import com.sipomeokjo.commitme.domain.user.repository.ResumeProfileRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserActivityRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserCertificateRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserEducationRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserExperienceRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserTechStackRepository;
import com.sipomeokjo.commitme.domain.user.service.UserFinder;
import com.sipomeokjo.commitme.global.mongo.MongoSequenceService;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProfileService {
    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final UserFinder userFinder;
    private final PositionFinder positionFinder;
    private final MongoSequenceService mongoSequenceService;
    private final UserTechStackRepository userTechStackRepository;
    private final UserExperienceRepository userExperienceRepository;
    private final UserEducationRepository userEducationRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserCertificateRepository userCertificateRepository;
    private final UserValidationExceptionMapper validationExceptionMapper;
    private final S3UploadService s3UploadService;
    private final ResumeProfileMapper resumeProfileMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ResumeProjectionService resumeProjectionService;

    @Transactional
    public ResumeProfileCreateResponse createProfile(Long userId, ResumeProfileRequest request) {
        User user = userFinder.getByIdOrThrow(userId);
        Position position = positionFinder.getByIdOrThrow(user.getPosition().getId());
        validateProfileRequest(request);
        updateUserContact(user, request);

        Long resumeId = mongoSequenceService.nextResumeId();
        String resumeName = request.name().trim() + " 이력서";
        String profileSnapshot = buildSnapshotFromRequest(resumeId, request, user);
        Instant now = Instant.now(clock);

        ResumeEventDocument event =
                ResumeEventDocument.create(resumeId, 1, userId, ResumeVersionStatus.QUEUED, "{}");
        event.succeed("{}", now);
        resumeEventMongoRepository.save(event);

        ResumeDocument projection =
                ResumeDocument.create(
                        resumeId,
                        userId,
                        position.getId(),
                        position.getName(),
                        null,
                        null,
                        resumeName,
                        profileSnapshot);
        resumeProjectionService.createProjection(projection);
        resumeProjectionService.applyAiSuccess(resumeId, 1, true);
        resumeProjectionService.applyProfileSnapshotUpdate(resumeId, profileSnapshot);

        return new ResumeProfileCreateResponse(resumeId);
    }

    @Transactional
    public ResumeProfileUpdateResponse updateProfile(
            Long userId, Long resumeId, ResumeProfileRequest request) {
        resumeProjectionService.findDocumentByResumeIdAndUserIdOrThrow(resumeId, userId);
        User user = userFinder.getByIdOrThrow(userId);
        validateProfileRequest(request);
        updateUserContact(user, request);
        String snapshot = buildSnapshotFromRequest(resumeId, request, user);
        resumeProjectionService.applyProfileSnapshotUpdate(resumeId, snapshot);
        return new ResumeProfileUpdateResponse(resumeId, Instant.now(clock));
    }

    @Transactional
    public ResumeProfileUpdateResponse updateDefaultProfile(
            Long userId, ResumeProfileRequest request) {
        User user = userFinder.getByIdOrThrow(userId);
        validateProfileRequest(request);
        updateUserContact(user, request);
        Long latestResumeId =
                resumeMongoRepository
                        .findTopByUserIdOrderByUpdatedAtDescResumeIdDesc(userId)
                        .map(ResumeDocument::getResumeId)
                        .orElse(null);
        return new ResumeProfileUpdateResponse(latestResumeId, Instant.now(clock));
    }

    @Transactional
    public ResumeProfileResponse getProfile(Long userId) {
        return resumeMongoRepository
                .findTopByUserIdOrderByUpdatedAtDescResumeIdDesc(userId)
                .map(doc -> getProfile(userId, doc.getResumeId(), doc.getProfileSnapshot()))
                .orElseGet(() -> buildEmptyProfileResponse(userId));
    }

    @Transactional
    public ResumeProfileResponse getProfile(Long userId, Long resumeId) {
        ResumeDocument doc =
                resumeMongoRepository
                        .findByResumeId(resumeId)
                        .filter(d -> d.getUserId().equals(userId))
                        .or(
                                () ->
                                        resumeMongoRepository
                                                .findTopByUserIdOrderByUpdatedAtDescResumeIdDesc(
                                                        userId))
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
        return getProfile(userId, doc.getResumeId(), doc.getProfileSnapshot());
    }

    @Transactional
    public ResumeProfileResponse getProfile(Long userId, Long resumeId, String profileSnapshot) {
        ResumeProfileResponse snapshotResponse = readSnapshotResponse(profileSnapshot);
        if (snapshotResponse != null) {
            return withResumeId(resumeId, snapshotResponse);
        }
        return buildLiveProfileResponse(userId, resumeId);
    }

    private ResumeProfileResponse buildEmptyProfileResponse(Long userId) {
        User user = userFinder.getByIdOrThrow(userId);
        ResumeProfile profile = resolveOrCreateProfile(user);
        return new ResumeProfileResponse(
                null,
                user.getName(),
                s3UploadService.toCdnUrl(user.getProfileImageUrl()),
                profile.getPhoneCountryCode(),
                profile.getPhoneNationalNumber(),
                profile.getSummary(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private ResumeProfileResponse buildLiveProfileResponse(Long userId, Long resumeId) {
        User user = userFinder.getByIdOrThrow(userId);
        ResumeProfile profile = resolveOrCreateProfile(user);
        List<UserTechStack> techStacks = userTechStackRepository.findAllByUser_Id(userId);
        List<UserExperience> experiences =
                userExperienceRepository.findAllByUser_IdOrderByCreatedAtAsc(userId);
        List<UserEducation> educations =
                userEducationRepository.findAllByUser_IdOrderByIdAsc(userId);
        List<UserActivity> activities = userActivityRepository.findAllByUser_IdOrderByIdAsc(userId);
        List<UserCertificate> certificates =
                userCertificateRepository.findAllByUser_IdOrderByIdAsc(userId);

        return new ResumeProfileResponse(
                resumeId,
                user.getName(),
                s3UploadService.toCdnUrl(user.getProfileImageUrl()),
                profile.getPhoneCountryCode(),
                profile.getPhoneNationalNumber(),
                profile.getSummary(),
                resumeProfileMapper.toTechStackResponses(techStacks),
                resumeProfileMapper.toExperienceResponses(experiences),
                resumeProfileMapper.toEducationResponses(educations),
                resumeProfileMapper.toActivityResponses(activities),
                resumeProfileMapper.toCertificateResponses(certificates));
    }

    private String buildSnapshotFromRequest(
            Long resumeId, ResumeProfileRequest request, User user) {
        List<ResumeProfileResponse.TechStackResponse> techStackResponses = new ArrayList<>();
        if (request.techStacks() != null) {
            int idx = 0;
            for (ResumeProfileRequest.TechStackRequest req : request.techStacks()) {
                if (req == null || req.name() == null || req.name().isBlank()) {
                    continue;
                }
                techStackResponses.add(
                        new ResumeProfileResponse.TechStackResponse(
                                (long) (idx + 1), req.name().trim()));
                idx++;
            }
        }

        List<ResumeProfileResponse.ExperienceResponse> experienceResponses = new ArrayList<>();
        if (request.experiences() != null) {
            for (int i = 0; i < request.experiences().size(); i++) {
                ResumeProfileRequest.ExperienceRequest req = request.experiences().get(i);
                validateExperienceRequest(req);
                boolean isCurrent = Boolean.TRUE.equals(req.isCurrentlyWorking());
                experienceResponses.add(
                        new ResumeProfileResponse.ExperienceResponse(
                                (long) (i + 1),
                                req.companyName().trim(),
                                req.position().trim(),
                                normalizeNullable(req.department()),
                                req.startAt(),
                                isCurrent ? null : req.endAt(),
                                isCurrent,
                                parseEmploymentType(req.employmentType()).name(),
                                req.responsibilities()));
            }
        }

        List<ResumeProfileResponse.EducationResponse> educationResponses = new ArrayList<>();
        if (request.educations() != null) {
            for (int i = 0; i < request.educations().size(); i++) {
                ResumeProfileRequest.EducationRequest req = request.educations().get(i);
                validateEducationRequest(req);
                educationResponses.add(
                        new ResumeProfileResponse.EducationResponse(
                                (long) (i + 1),
                                parseEducationType(req.educationType()).name(),
                                req.institution().trim(),
                                req.major().trim(),
                                parseEducationStatus(req.status()).name(),
                                req.startAt(),
                                req.endAt()));
            }
        }

        List<ResumeProfileResponse.ActivityResponse> activityResponses = new ArrayList<>();
        if (request.activities() != null) {
            for (int i = 0; i < request.activities().size(); i++) {
                ResumeProfileRequest.ActivityRequest req = request.activities().get(i);
                validateActivityRequest(req);
                activityResponses.add(
                        new ResumeProfileResponse.ActivityResponse(
                                (long) (i + 1),
                                req.title().trim(),
                                req.organization().trim(),
                                req.year(),
                                req.description()));
            }
        }

        List<ResumeProfileResponse.CertificateResponse> certificateResponses = new ArrayList<>();
        if (request.certificates() != null) {
            for (int i = 0; i < request.certificates().size(); i++) {
                ResumeProfileRequest.CertificateRequest req = request.certificates().get(i);
                validateCertificateRequest(req);
                certificateResponses.add(
                        new ResumeProfileResponse.CertificateResponse(
                                (long) (i + 1),
                                req.name().trim(),
                                req.score(),
                                req.issuer(),
                                req.issuedAt()));
            }
        }

        ResumeProfileResponse snapshot =
                new ResumeProfileResponse(
                        resumeId,
                        request.name().trim(),
                        request.profileImageUrl(),
                        normalizeNullable(request.phoneCountryCode()),
                        normalizeNullable(request.phoneNumber()),
                        normalizeNullable(request.introduction()),
                        techStackResponses,
                        experienceResponses,
                        educationResponses,
                        activityResponses,
                        certificateResponses);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn(
                    "[RESUME_PROFILE] snapshot_serialize_failed userId={} resumeId={} error={}",
                    user.getId(),
                    resumeId,
                    e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ResumeProfileResponse readSnapshotResponse(String profileSnapshot) {
        if (profileSnapshot == null || profileSnapshot.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(profileSnapshot, ResumeProfileResponse.class);
        } catch (Exception e) {
            log.warn("[RESUME_PROFILE] snapshot_deserialize_failed error={}", e.getMessage());
            return null;
        }
    }

    private ResumeProfileResponse withResumeId(Long resumeId, ResumeProfileResponse response) {
        return new ResumeProfileResponse(
                resumeId,
                response.name(),
                response.profileImageUrl(),
                response.phoneCountryCode(),
                response.phoneNumber(),
                response.introduction(),
                response.techStacks(),
                response.experiences(),
                response.educations(),
                response.activities(),
                response.certificates());
    }

    private ResumeProfile resolveOrCreateProfile(User user) {
        return resumeProfileRepository
                .findById(user.getId())
                .orElseGet(
                        () ->
                                resumeProfileRepository.save(
                                        ResumeProfile.create(user, null, null, null)));
    }

    private void updateUserContact(User user, ResumeProfileRequest request) {
        String userName = request.name().trim();
        String normalizedPhoneNumber = normalizeNullable(request.phoneNumber());
        String profileImageKey = s3UploadService.toS3Key(request.profileImageUrl());
        try {
            user.updateProfile(
                    user.getPosition(), userName, normalizedPhoneNumber, profileImageKey);
        } catch (UserProfileValidationException ex) {
            throw validationExceptionMapper.toBusinessException(ex);
        }
    }

    private YearMonth parseYearMonth(String value, boolean allowNull) {
        if (value == null || value.isBlank()) {
            if (allowNull) {
                return null;
            }

            throw new BusinessException(ErrorCode.RESUME_PROFILE_DATE_INVALID);
        }
        try {
            String normalized = value.trim();
            String[] parts = normalized.split("\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException();
            }
            return YearMonth.of(year, month);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_DATE_INVALID);
        }
    }

    private void validateYearMonth(String value, boolean allowNull) {
        parseYearMonth(value, allowNull);
    }

    private EmploymentType parseEmploymentType(String value) {
        try {
            return EmploymentType.fromValue(value);
        } catch (EnumParseException ex) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EMPLOYMENT_TYPE_INVALID);
        }
    }

    private EducationType parseEducationType(String value) {
        try {
            return EducationType.fromValue(value);
        } catch (EnumParseException ex) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EDUCATION_TYPE_INVALID);
        }
    }

    private EducationStatus parseEducationStatus(String value) {
        try {
            return EducationStatus.fromValue(value);
        } catch (EnumParseException ex) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EDUCATION_STATUS_INVALID);
        }
    }

    private String normalizeNullable(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateProfileRequest(ResumeProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_RESUME_PROFILE_INPUT);
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_NAME_REQUIRED);
        }
        if (request.name().trim().length() > 10) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_NAME_LENGTH_OUT_OF_RANGE);
        }
        validateArraySizeLimits(request);
    }

    private void validateArraySizeLimits(ResumeProfileRequest request) {
        if (request.techStacks() != null && request.techStacks().size() > 10) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_TECH_STACK_LIMIT_EXCEEDED);
        }
        if (request.experiences() != null && request.experiences().size() > 5) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EXPERIENCE_LIMIT_EXCEEDED);
        }
        if (request.educations() != null && request.educations().size() > 5) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EDUCATION_LIMIT_EXCEEDED);
        }
        if (request.activities() != null && request.activities().size() > 10) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_ACTIVITY_LIMIT_EXCEEDED);
        }
        if (request.certificates() != null && request.certificates().size() > 10) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_CERTIFICATE_LIMIT_EXCEEDED);
        }
    }

    private void validateExperienceRequest(ResumeProfileRequest.ExperienceRequest req) {
        if (req == null
                || req.companyName() == null
                || req.position() == null
                || req.startAt() == null
                || req.isCurrentlyWorking() == null
                || req.employmentType() == null
                || req.responsibilities() == null) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EXPERIENCE_INVALID);
        }
        if (req.isCurrentlyWorking() && req.endAt() != null) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EXPERIENCE_INVALID);
        }
        if (!req.isCurrentlyWorking() && (req.endAt() == null || req.endAt().isBlank())) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EXPERIENCE_INVALID);
        }
        validateYearMonth(req.startAt(), false);
        validateYearMonth(req.endAt(), req.isCurrentlyWorking());
    }

    private void validateEducationRequest(ResumeProfileRequest.EducationRequest req) {
        if (req == null
                || req.educationType() == null
                || req.institution() == null
                || req.major() == null
                || req.status() == null
                || req.startAt() == null
                || req.endAt() == null) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_EDUCATION_INVALID);
        }
        validateYearMonth(req.startAt(), false);
        validateYearMonth(req.endAt(), false);
    }

    private void validateActivityRequest(ResumeProfileRequest.ActivityRequest req) {
        if (req == null
                || req.title() == null
                || req.organization() == null
                || req.year() == null
                || req.description() == null) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_ACTIVITY_INVALID);
        }
    }

    private void validateCertificateRequest(ResumeProfileRequest.CertificateRequest req) {
        if (req == null || req.name() == null || req.issuedAt() == null) {
            throw new BusinessException(ErrorCode.RESUME_PROFILE_CERTIFICATE_INVALID);
        }
        validateYearMonth(req.issuedAt(), false);
    }
}
