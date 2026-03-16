package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.service.PositionFinder;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.mapper.ResumeProfileMapper;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.upload.service.S3UploadService;
import com.sipomeokjo.commitme.domain.user.entity.ResumeProfile;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.mapper.UserValidationExceptionMapper;
import com.sipomeokjo.commitme.domain.user.repository.ResumeProfileRepository;
import com.sipomeokjo.commitme.domain.user.repository.TechStackRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserActivityRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserCertificateRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserEducationRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserExperienceRepository;
import com.sipomeokjo.commitme.domain.user.repository.UserTechStackRepository;
import com.sipomeokjo.commitme.domain.user.service.UserFinder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResumeProfileServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeVersionRepository resumeVersionRepository;
    @Mock private ResumeProfileRepository resumeProfileRepository;
    @Mock private UserFinder userFinder;
    @Mock private PositionFinder positionFinder;
    @Mock private ResumeFinder resumeFinder;
    @Mock private TechStackRepository techStackRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private UserExperienceRepository userExperienceRepository;
    @Mock private UserEducationRepository userEducationRepository;
    @Mock private UserActivityRepository userActivityRepository;
    @Mock private UserCertificateRepository userCertificateRepository;
    @Mock private UserValidationExceptionMapper validationExceptionMapper;
    @Mock private S3UploadService s3UploadService;

    private ResumeProfileService resumeProfileService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-03-16T00:00:00Z"), ZoneOffset.UTC);
        resumeProfileService =
                new ResumeProfileService(
                        resumeRepository,
                        resumeVersionRepository,
                        resumeProfileRepository,
                        userFinder,
                        positionFinder,
                        resumeFinder,
                        techStackRepository,
                        userTechStackRepository,
                        userExperienceRepository,
                        userEducationRepository,
                        userActivityRepository,
                        userCertificateRepository,
                        validationExceptionMapper,
                        s3UploadService,
                        new ResumeProfileMapper(),
                        objectMapper,
                        clock);
    }

    @Test
    void getProfile_prefersResumeSnapshotWithoutLiveProfileQueries() throws Exception {
        Long userId = 1L;
        Long resumeId = 10L;
        Resume resume = createResume(userId, resumeId, "현재이름", "current-profile-key");
        resume.updateProfileSnapshot(
                objectMapper.writeValueAsString(
                        new ResumeProfileResponse(
                                999L,
                                "스냅샷이름",
                                "https://cdn.commit-me.com/snapshot.png",
                                "+82",
                                "01012345678",
                                "스냅샷 소개",
                                List.of(
                                        new ResumeProfileResponse.TechStackResponse(
                                                1L, "Spring Boot")),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of())));

        given(resumeRepository.findByIdAndUser_Id(resumeId, userId))
                .willReturn(Optional.of(resume));

        ResumeProfileResponse response = resumeProfileService.getProfile(userId, resumeId);

        assertThat(response.resumeId()).isEqualTo(resumeId);
        assertThat(response.name()).isEqualTo("스냅샷이름");
        assertThat(response.profileImageUrl()).isEqualTo("https://cdn.commit-me.com/snapshot.png");
        assertThat(response.techStacks()).hasSize(1);
        verifyNoInteractions(resumeProfileRepository);
        verifyNoInteractions(userTechStackRepository);
        verifyNoInteractions(userExperienceRepository);
        verifyNoInteractions(userEducationRepository);
        verifyNoInteractions(userActivityRepository);
        verifyNoInteractions(userCertificateRepository);
        verifyNoInteractions(s3UploadService);
    }

    @Test
    void getProfile_withoutResumeSnapshot_fallsBackToLiveUserProfile() {
        Long userId = 1L;
        Long resumeId = 10L;
        Resume resume = createResume(userId, resumeId, "홍길동", "profiles/live.png");
        ResumeProfile profile =
                ResumeProfile.create(resume.getUser(), "+82", "01012345678", "기본 소개");

        given(resumeRepository.findByIdAndUser_Id(resumeId, userId))
                .willReturn(Optional.of(resume));
        given(resumeProfileRepository.findById(userId)).willReturn(Optional.of(profile));
        given(userTechStackRepository.findAllByUser_IdOrderByCreatedAtAsc(userId))
                .willReturn(List.of());
        given(userExperienceRepository.findAllByUser_IdOrderByCreatedAtAsc(userId))
                .willReturn(List.of());
        given(userEducationRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());
        given(userActivityRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());
        given(userCertificateRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());
        given(s3UploadService.toCdnUrl("profiles/live.png"))
                .willReturn("https://cdn.commit-me.com/profiles/live.png");

        ResumeProfileResponse response = resumeProfileService.getProfile(userId, resumeId);

        assertThat(response.resumeId()).isEqualTo(resumeId);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.profileImageUrl())
                .isEqualTo("https://cdn.commit-me.com/profiles/live.png");
        assertThat(response.phoneCountryCode()).isEqualTo("+82");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.introduction()).isEqualTo("기본 소개");
    }

    @Test
    void updateProfile_writesResumeSpecificSnapshot() throws Exception {
        Long userId = 1L;
        Long resumeId = 10L;
        Resume resume = createResume(userId, resumeId, "기존이름", "profiles/old.png");
        ResumeProfileRequest request =
                new ResumeProfileRequest(
                        "홍길동",
                        "https://cdn.commit-me.com/profiles/new.png",
                        "+82",
                        "01012345678",
                        "이력서 전용 소개",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of());
        ResumeProfile savedProfile =
                ResumeProfile.create(resume.getUser(), "+82", "01012345678", "이력서 전용 소개");

        given(resumeFinder.getByIdAndUserIdOrThrow(resumeId, userId)).willReturn(resume);
        given(resumeProfileRepository.findById(userId))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(savedProfile));
        given(resumeProfileRepository.save(any(ResumeProfile.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(s3UploadService.toS3Key("https://cdn.commit-me.com/profiles/new.png"))
                .willReturn("profiles/new.png");
        given(s3UploadService.toCdnUrl("profiles/new.png"))
                .willReturn("https://cdn.commit-me.com/profiles/new.png");
        given(userTechStackRepository.findAllByUser_IdOrderByCreatedAtAsc(userId))
                .willReturn(List.of());
        given(userExperienceRepository.findAllByUser_IdOrderByCreatedAtAsc(userId))
                .willReturn(List.of());
        given(userEducationRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());
        given(userActivityRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());
        given(userCertificateRepository.findAllByUser_IdOrderByIdAsc(userId)).willReturn(List.of());

        resumeProfileService.updateProfile(userId, resumeId, request);

        assertThat(resume.getProfileSnapshot()).isNotBlank();
        ResumeProfileResponse snapshot =
                objectMapper.readValue(resume.getProfileSnapshot(), ResumeProfileResponse.class);
        assertThat(snapshot.resumeId()).isEqualTo(resumeId);
        assertThat(snapshot.name()).isEqualTo("홍길동");
        assertThat(snapshot.profileImageUrl())
                .isEqualTo("https://cdn.commit-me.com/profiles/new.png");
        assertThat(snapshot.phoneCountryCode()).isEqualTo("+82");
        assertThat(snapshot.phoneNumber()).isEqualTo("01012345678");
        assertThat(snapshot.introduction()).isEqualTo("이력서 전용 소개");
    }

    private Resume createResume(Long userId, Long resumeId, String name, String profileImageUrl) {
        Position position = org.mockito.Mockito.mock(Position.class);
        User user =
                User.builder()
                        .id(userId)
                        .position(position)
                        .name(name)
                        .phone("01011112222")
                        .profileImageUrl(profileImageUrl)
                        .status(UserStatus.ACTIVE)
                        .build();
        Resume resume = Resume.create(user, position, null, "백엔드 이력서");
        ReflectionTestUtils.setField(resume, "id", resumeId);
        return resume;
    }
}
