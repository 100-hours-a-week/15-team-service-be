package com.sipomeokjo.commitme.domain.resume.dto;

import java.time.Instant;
import java.util.List;

public record ResumeDetailDto(
        Long resumeId,
        String name,
        boolean isEditing,
        Long positionId,
        String positionName,
        Long companyId,
        String companyName,
        Integer currentVersionNo,
        String content,
        ResumeDetailProfileDto profile,
        Instant commitedAt,
        Instant createdAt,
        Instant updatedAt) {

    public record ResumeDetailProfileDto(
            String name,
            String profileImageUrl,
            String phoneCountryCode,
            String phoneNumber,
            String introduction,
            List<ResumeProfileResponse.TechStackResponse> techStacks,
            List<ResumeProfileResponse.ExperienceResponse> experiences,
            List<ResumeProfileResponse.EducationResponse> educations,
            List<ResumeProfileResponse.ActivityResponse> activities,
            List<ResumeProfileResponse.CertificateResponse> certificates) {

        public static ResumeDetailProfileDto from(ResumeProfileResponse response) {
            if (response == null) {
                return null;
            }

            return new ResumeDetailProfileDto(
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
    }
}
