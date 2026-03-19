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
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeProfileMapper;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoQueryRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.entity.CertificateType;
import com.sipomeokjo.commitme.domain.user.entity.EducationStatus;
import com.sipomeokjo.commitme.domain.user.entity.EducationType;
import com.sipomeokjo.commitme.domain.user.entity.EmploymentType;
import com.sipomeokjo.commitme.domain.user.entity.EnumParseException;
import com.sipomeokjo.commitme.domain.user.entity.ResumeProfile;
import com.sipomeokjo.commitme.domain.user.entity.ResumeProfileValidationException;
import com.sipomeokjo.commitme.domain.user.entity.TechStack;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserActivity;
import com.sipomeokjo.commitme.domain.user.entity.UserCertificate;
import com.sipomeokjo.commitme.domain.user.entity.UserEducation;
import com.sipomeokjo.commitme.domain.user.entity.UserExperience;
import com.sipomeokjo.commitme.domain.user.entity.UserProfileValidationException;
import com.sipomeokjo.commitme.domain.user.entity.UserTechStack;
import com.sipomeokjo.commitme.domain.user.mapper.UserValidationExceptionMapper;
import com.sipomeokjo.commitme.domain.user.repository.ResumeProfileRepository;
import com.sipomeokjo.commitme.domain.user.repository.TechStackRepository;
import com.sipomeokjo.commitme.domain.user.repository.TechStackUpsertRow;
import com.sipomeokjo.commitme.domain.user.repository.UserActivityRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserCertificateRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserEducationRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserExperienceRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserTechStackRepository;
import com.sipomeokjo.commitme.domain.user.service.UserFinder;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeProfileService {
    private final ResumeRepository resumeRepository;
    private final ResumeEventMongoRepository resumeEventMongoRepository;
    private final ResumeMongoRepository resumeMongoRepository;
    private final ResumeMongoQueryRepository resumeMongoQueryRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final UserFinder userFinder;
    private final PositionFinder positionFinder;
    private final ResumeFinder resumeFinder;
    private final TechStackRepository techStackRepository;
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

    @Transactional
    public ResumeProfileCreateResponse createProfile(Long userId, ResumeProfileRequest request) {
        User user = userFinder.getByIdOrThrow(userId);
        Position position = positionFinder.getByIdOrThrow(user.getPosition().getId());
        validateProfileRequest(request);

        Resume resume = Resume.create(user, position, null, request.name().trim() + " 이력서");
        resumeRepository.save(resume);

        ResumeEventDocument event =
                ResumeEventDocument.create(
                        resume.getId(), 1, userId, ResumeVersionStatus.QUEUED, "{}");
        event.succeed("{}", Instant.now(clock));
        resumeEventMongoRepository.save(event);
        persistProfileData(user, request);
        saveProfileSnapshot(userId, resume);
        return new ResumeProfileCreateResponse(resume.getId());
    }

    @Transactional
    public ResumeProfileUpdateResponse updateProfile(
            Long userId, Long resumeId, ResumeProfileRequest request) {
        Resume resume = resumeFinder.getByIdAndUserIdOrThrow(resumeId, userId);
        validateProfileRequest(request);
        persistProfileData(resume.getUser(), request);
        saveProfileSnapshot(userId, resume);
        resume.touchUpdatedAtNow();
        return new ResumeProfileUpdateResponse(resumeId, Instant.now(clock));
    }

    @Transactional
    public ResumeProfileUpdateResponse updateDefaultProfile(
            Long userId, ResumeProfileRequest request) {
        User user = userFinder.getByIdOrThrow(userId);
        validateProfileRequest(request);
        persistProfileData(user, request);
        Long latestResumeId =
                resumeMongoQueryRepository
                        .findByUserIdWithCursorDesc(userId, null, null, null, 1)
                        .stream()
                        .findFirst()
                        .map(ResumeDocument::getResumeId)
                        .orElse(null);
        return new ResumeProfileUpdateResponse(latestResumeId, Instant.now(clock));
    }

    @Transactional
    public ResumeProfileResponse getProfile(Long userId) {
        return resumeMongoQueryRepository
                .findByUserIdWithCursorDesc(userId, null, null, null, 1)
                .stream()
                .findFirst()
                .map(doc -> getProfile(userId, doc.getResumeId(), doc.getProfileSnapshot()))
                .orElseGet(() -> buildEmptyProfileResponse(userId));
    }

    @Transactional
    public ResumeProfileResponse getProfile(Long userId, Long resumeId) {
        var doc =
                resumeMongoRepository
                        .findByResumeId(resumeId)
                        .filter(d -> d.getUserId().equals(userId))
                        .orElseGet(
                                () ->
                                        resumeMongoQueryRepository
                                                .findByUserIdWithCursorDesc(
                                                        userId, null, null, null, 1)
                                                .stream()
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new BusinessException(
                                                                        ErrorCode
                                                                                .RESUME_NOT_FOUND)));
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

    @Transactional
    public ResumeProfileResponse getProfile(Long userId, Resume resume) {
        return getProfile(userId, resume.getId(), resume.getProfileSnapshot());
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

    private void saveProfileSnapshot(Long userId, Resume resume) {
        ResumeProfileResponse snapshot = buildLiveProfileResponse(userId, resume.getId());
        try {
            resume.updateProfileSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn(
                    "[RESUME_PROFILE] snapshot_serialize_failed userId={} resumeId={} error={}",
                    userId,
                    resume.getId(),
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

    private void persistProfileData(User user, ResumeProfileRequest request) {
        ResumeProfile profile =
                resumeProfileRepository
                        .findById(user.getId())
                        .orElseGet(() -> ResumeProfile.create(user, null, null, null));
        updateResumeProfile(profile, request);
        resumeProfileRepository.save(profile);
        updateUserContact(user, request);
        syncTechStacks(user, request.techStacks());
        syncExperiences(user, request.experiences());
        syncEducations(user, request.educations());
        syncActivities(user, request.activities());
        syncCertificates(user, request.certificates());
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

    private void syncTechStacks(User user, List<ResumeProfileRequest.TechStackRequest> requests) {
        userTechStackRepository.deleteAllByUser_Id(user.getId());
        if (requests == null || requests.isEmpty()) {
            return;
        }

        LinkedHashMap<String, String> normalizedToName = new LinkedHashMap<>();
        for (ResumeProfileRequest.TechStackRequest req : requests) {
            if (req == null || req.name() == null || req.name().isBlank()) {
                continue;
            }
            String normalized = normalizeTechName(req.name());
            if (normalized.isEmpty() || normalizedToName.containsKey(normalized)) {
                continue;
            }
            normalizedToName.put(normalized, req.name().trim());
            if (normalizedToName.size() >= 30) {
                break;
            }
        }

        Map<String, TechStack> techStackByNormalized =
                toTechStackByNormalized(
                        techStackRepository.findAllByNameNormalizedIn(normalizedToName.keySet()));
        Map<String, TechStack> techStackByName =
                toTechStackByName(
                        techStackRepository.findAllByNameIn(
                                new LinkedHashSet<>(normalizedToName.values())));

        List<TechStackUpsertRow> missingTechStacks = new ArrayList<>();
        for (Map.Entry<String, String> entry : normalizedToName.entrySet()) {
            if (techStackByNormalized.containsKey(entry.getKey())
                    || techStackByName.containsKey(entry.getValue())) {
                continue;
            }
            missingTechStacks.add(new TechStackUpsertRow(entry.getValue(), entry.getKey()));
        }

        if (!missingTechStacks.isEmpty()) {
            techStackRepository.upsertAll(missingTechStacks);
            techStackByNormalized.putAll(
                    toTechStackByNormalized(
                            techStackRepository.findAllByNameNormalizedIn(
                                    normalizedToName.keySet())));
            techStackByName.putAll(
                    toTechStackByName(
                            techStackRepository.findAllByNameIn(
                                    new LinkedHashSet<>(normalizedToName.values()))));
        }

        List<UserTechStack> userTechStacks = new ArrayList<>();
        for (Map.Entry<String, String> entry : normalizedToName.entrySet()) {
            TechStack techStack = techStackByNormalized.get(entry.getKey());
            if (techStack == null) {
                techStack = techStackByName.get(entry.getValue());
            }
            if (techStack != null) {
                userTechStacks.add(UserTechStack.create(user, techStack));
            }
        }
        if (!userTechStacks.isEmpty()) {
            userTechStackRepository.saveAll(userTechStacks);
        }
    }

    private Map<String, TechStack> toTechStackByNormalized(List<TechStack> techStacks) {
        Map<String, TechStack> techStackByNormalized = new HashMap<>();
        for (TechStack techStack : techStacks) {
            techStackByNormalized.put(techStack.getNameNormalized(), techStack);
        }
        return techStackByNormalized;
    }

    private Map<String, TechStack> toTechStackByName(List<TechStack> techStacks) {
        Map<String, TechStack> techStackByName = new HashMap<>();
        for (TechStack techStack : techStacks) {
            techStackByName.put(techStack.getName(), techStack);
        }
        return techStackByName;
    }

    private void syncExperiences(User user, List<ResumeProfileRequest.ExperienceRequest> requests) {
        List<UserExperience> existing =
                userExperienceRepository.findAllByUser_IdOrderByCreatedAtAsc(user.getId());
        syncEntitiesById(
                requests,
                5,
                ErrorCode.RESUME_PROFILE_EXPERIENCE_LIMIT_EXCEEDED,
                existing,
                ResumeProfileRequest.ExperienceRequest::id,
                UserExperience::getId,
                req -> {
                    validateExperienceRequest(req);

                    YearMonth start = parseYearMonth(req.startAt(), false);
                    boolean isCurrent = Boolean.TRUE.equals(req.isCurrentlyWorking());
                    YearMonth end = parseYearMonth(req.endAt(), isCurrent);
                    EmploymentType employmentType = parseEmploymentType(req.employmentType());
                    return UserExperience.create(
                            user,
                            req.companyName().trim(),
                            req.position().trim(),
                            normalizeNullable(req.department()),
                            employmentType,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            end == null ? null : (short) end.getYear(),
                            end == null ? null : (byte) end.getMonthValue(),
                            isCurrent,
                            req.responsibilities().trim());
                },
                (experience, req) -> {
                    validateExperienceRequest(req);

                    YearMonth start = parseYearMonth(req.startAt(), false);
                    boolean isCurrent = Boolean.TRUE.equals(req.isCurrentlyWorking());
                    YearMonth end = parseYearMonth(req.endAt(), isCurrent);
                    EmploymentType employmentType = parseEmploymentType(req.employmentType());
                    String companyName = req.companyName().trim();
                    String positionTitle = req.position().trim();
                    String departmentName = normalizeNullable(req.department());
                    Short endYear = end == null ? null : (short) end.getYear();
                    Byte endMonth = end == null ? null : (byte) end.getMonthValue();
                    String description = req.responsibilities().trim();
                    if (!isExperienceChanged(
                            experience,
                            companyName,
                            positionTitle,
                            departmentName,
                            employmentType,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            endYear,
                            endMonth,
                            isCurrent,
                            description)) {
                        return;
                    }
                    experience.update(
                            companyName,
                            positionTitle,
                            departmentName,
                            employmentType,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            endYear,
                            endMonth,
                            isCurrent,
                            description);
                },
                userExperienceRepository::saveAll,
                userExperienceRepository::deleteAllInBatch);
    }

    private void syncEducations(User user, List<ResumeProfileRequest.EducationRequest> requests) {
        List<UserEducation> existing =
                userEducationRepository.findAllByUser_IdOrderByIdAsc(user.getId());
        syncEntitiesById(
                requests,
                5,
                ErrorCode.RESUME_PROFILE_EDUCATION_LIMIT_EXCEEDED,
                existing,
                ResumeProfileRequest.EducationRequest::id,
                UserEducation::getId,
                req -> {
                    validateEducationRequest(req);
                    YearMonth start = parseYearMonth(req.startAt(), false);
                    YearMonth end = parseYearMonth(req.endAt(), false);
                    EducationType educationType = parseEducationType(req.educationType());
                    EducationStatus educationStatus = parseEducationStatus(req.status());
                    return UserEducation.create(
                            user,
                            educationType,
                            req.institution().trim(),
                            req.major().trim(),
                            educationStatus,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            (short) end.getYear(),
                            (byte) end.getMonthValue());
                },
                (education, req) -> {
                    validateEducationRequest(req);
                    YearMonth start = parseYearMonth(req.startAt(), false);
                    YearMonth end = parseYearMonth(req.endAt(), false);
                    EducationType educationType = parseEducationType(req.educationType());
                    EducationStatus educationStatus = parseEducationStatus(req.status());
                    String institution = req.institution().trim();
                    String major = req.major().trim();
                    if (!isEducationChanged(
                            education,
                            educationType,
                            institution,
                            major,
                            educationStatus,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            (short) end.getYear(),
                            (byte) end.getMonthValue())) {
                        return;
                    }
                    education.update(
                            educationType,
                            institution,
                            major,
                            educationStatus,
                            (short) start.getYear(),
                            (byte) start.getMonthValue(),
                            (short) end.getYear(),
                            (byte) end.getMonthValue());
                },
                userEducationRepository::saveAll,
                userEducationRepository::deleteAllInBatch);
    }

    private void syncActivities(User user, List<ResumeProfileRequest.ActivityRequest> requests) {
        List<UserActivity> existing =
                userActivityRepository.findAllByUser_IdOrderByIdAsc(user.getId());
        syncEntitiesById(
                requests,
                10,
                ErrorCode.RESUME_PROFILE_ACTIVITY_LIMIT_EXCEEDED,
                existing,
                ResumeProfileRequest.ActivityRequest::id,
                UserActivity::getId,
                req -> {
                    validateActivityRequest(req);
                    return UserActivity.create(
                            user,
                            req.title().trim(),
                            req.organization().trim(),
                            req.year() == null ? null : req.year().shortValue(),
                            req.description().trim());
                },
                (activity, req) -> {
                    validateActivityRequest(req);
                    String title = req.title().trim();
                    String organization = req.organization().trim();
                    Short year = req.year() == null ? null : req.year().shortValue();
                    String description = req.description().trim();
                    if (!isActivityChanged(activity, title, organization, year, description)) {
                        return;
                    }
                    activity.update(title, organization, year, description);
                },
                userActivityRepository::saveAll,
                userActivityRepository::deleteAllInBatch);
    }

    private void syncCertificates(
            User user, List<ResumeProfileRequest.CertificateRequest> requests) {
        List<UserCertificate> existing =
                userCertificateRepository.findAllByUser_IdOrderByIdAsc(user.getId());
        syncEntitiesById(
                requests,
                10,
                ErrorCode.RESUME_PROFILE_CERTIFICATE_LIMIT_EXCEEDED,
                existing,
                ResumeProfileRequest.CertificateRequest::id,
                UserCertificate::getId,
                req -> {
                    validateCertificateRequest(req);
                    YearMonth issuedAt = parseYearMonth(req.issuedAt(), false);
                    return UserCertificate.create(
                            user,
                            CertificateType.CERTIFICATE,
                            req.name().trim(),
                            req.score(),
                            req.issuer(),
                            (short) issuedAt.getYear(),
                            (byte) issuedAt.getMonthValue());
                },
                (certificate, req) -> {
                    validateCertificateRequest(req);
                    YearMonth issuedAt = parseYearMonth(req.issuedAt(), false);
                    String title = req.name().trim();
                    String score = req.score();
                    String issuer = req.issuer();
                    Short year = (short) issuedAt.getYear();
                    Byte month = (byte) issuedAt.getMonthValue();
                    if (!isCertificateChanged(certificate, title, score, issuer, year, month)) {
                        return;
                    }
                    certificate.update(
                            CertificateType.CERTIFICATE, title, score, issuer, year, month);
                },
                userCertificateRepository::saveAll,
                userCertificateRepository::deleteAllInBatch);
    }

    private <R, E> void syncEntitiesById(
            List<R> requests,
            int maxCount,
            ErrorCode maxCountErrorCode,
            List<E> existing,
            Function<R, Long> requestIdExtractor,
            Function<E, Long> existingIdExtractor,
            Function<R, E> creator,
            BiConsumer<E, R> updater,
            Consumer<List<E>> batchSaver,
            Consumer<List<E>> batchDeleter) {
        if (requests == null || requests.isEmpty()) {
            if (!existing.isEmpty()) {
                batchDeleter.accept(existing);
            }
            return;
        }

        Map<Long, E> existingById = toMapById(existing, existingIdExtractor);
        Set<Long> keepIds = new HashSet<>();
        List<E> toCreate = new ArrayList<>();

        int count = 0;
        for (R request : requests) {
            if (count >= maxCount) {
                throw new BusinessException(maxCountErrorCode);
            }

            Long requestId = requestIdExtractor.apply(request);
            if (requestId == null) {
                toCreate.add(creator.apply(request));
            } else {
                E entity = existingById.get(requestId);
                if (entity == null) {
                    throw new BusinessException(ErrorCode.RESUME_PROFILE_ITEM_ID_INVALID);
                }
                updater.accept(entity, request);
                keepIds.add(existingIdExtractor.apply(entity));
            }
            count++;
        }

        if (!toCreate.isEmpty()) {
            batchSaver.accept(toCreate);
        }
        deleteRemoved(existing, keepIds, existingIdExtractor, batchDeleter);
    }

    private boolean isExperienceChanged(
            UserExperience experience,
            String companyName,
            String positionTitle,
            String departmentName,
            EmploymentType employmentType,
            Short startYear,
            Byte startMonth,
            Short endYear,
            Byte endMonth,
            boolean isCurrent,
            String description) {
        return !Objects.equals(experience.getCompanyName(), companyName)
                || !Objects.equals(experience.getPositionTitle(), positionTitle)
                || !Objects.equals(experience.getDepartmentName(), departmentName)
                || experience.getEmploymentType() != employmentType
                || !Objects.equals(experience.getStartYear(), startYear)
                || !Objects.equals(experience.getStartMonth(), startMonth)
                || !Objects.equals(experience.getEndYear(), endYear)
                || !Objects.equals(experience.getEndMonth(), endMonth)
                || experience.isCurrent() != isCurrent
                || !Objects.equals(experience.getDescription(), description);
    }

    private boolean isEducationChanged(
            UserEducation education,
            EducationType educationType,
            String institution,
            String major,
            EducationStatus status,
            Short startYear,
            Byte startMonth,
            Short endYear,
            Byte endMonth) {
        return education.getType() != educationType
                || !Objects.equals(education.getInstitution(), institution)
                || !Objects.equals(education.getMajorField(), major)
                || education.getStatus() != status
                || !Objects.equals(education.getStartYear(), startYear)
                || !Objects.equals(education.getStartMonth(), startMonth)
                || !Objects.equals(education.getEndYear(), endYear)
                || !Objects.equals(education.getEndMonth(), endMonth);
    }

    private boolean isActivityChanged(
            UserActivity activity,
            String title,
            String organization,
            Short year,
            String description) {
        return !Objects.equals(activity.getActivityName(), title)
                || !Objects.equals(activity.getOrganization(), organization)
                || !Objects.equals(activity.getActivityYear(), year)
                || !Objects.equals(activity.getDescription(), description);
    }

    private boolean isCertificateChanged(
            UserCertificate certificate,
            String title,
            String score,
            String issuer,
            Short year,
            Byte month) {
        return !Objects.equals(certificate.getTitle(), title)
                || !Objects.equals(certificate.getGradeOrScore(), score)
                || !Objects.equals(certificate.getIssuer(), issuer)
                || !Objects.equals(certificate.getAcquiredYear(), year)
                || !Objects.equals(certificate.getAcquiredMonth(), month);
    }

    private <E> Map<Long, E> toMapById(List<E> entities, Function<E, Long> idExtractor) {
        Map<Long, E> byId = new HashMap<>();
        for (E entity : entities) {
            byId.put(idExtractor.apply(entity), entity);
        }
        return byId;
    }

    private <E> void deleteRemoved(
            List<E> existing,
            Set<Long> keepIds,
            Function<E, Long> existingIdExtractor,
            Consumer<List<E>> batchDeleter) {
        if (existing.isEmpty()) {
            return;
        }

        List<E> deleteTargets =
                existing.stream()
                        .filter(entity -> !keepIds.contains(existingIdExtractor.apply(entity)))
                        .toList();
        if (!deleteTargets.isEmpty()) {
            batchDeleter.accept(deleteTargets);
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

    private String normalizeTechName(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
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
    }

    private void updateResumeProfile(ResumeProfile profile, ResumeProfileRequest request) {
        try {
            profile.update(
                    request.phoneCountryCode(), request.phoneNumber(), request.introduction());
        } catch (ResumeProfileValidationException ex) {
            throw validationExceptionMapper.toBusinessException(ex);
        }
    }
}
