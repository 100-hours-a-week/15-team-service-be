package com.sipomeokjo.commitme.domain.resume.dto;

import java.util.List;

public record ResumeProfileResponse(
        Long resumeId,
        String name,
        String profileImageUrl,
        String phoneCountryCode,
        String phoneNumber,
        String introduction,
        List<TechStackResponse> techStacks,
        List<ExperienceResponse> experiences,
        List<EducationResponse> educations,
        List<ActivityResponse> activities,
        List<CertificateResponse> certificates) {

    public record TechStackResponse(Long id, String name) {}

    public record ExperienceResponse(
            Long id,
            String companyName,
            String position,
            String department,
            String startAt,
            String endAt,
            Boolean isCurrentlyWorking,
            String employmentType,
            String responsibilities) {}

    public record EducationResponse(
            Long id,
            String educationType,
            String institution,
            String major,
            String status,
            String startAt,
            String endAt) {}

    public record ActivityResponse(
            Long id, String title, String organization, Integer year, String description) {}

    public record CertificateResponse(
            Long id, String name, String score, String issuer, String issuedAt) {}
}
