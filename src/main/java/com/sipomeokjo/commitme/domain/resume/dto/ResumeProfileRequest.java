package com.sipomeokjo.commitme.domain.resume.dto;

import java.util.List;

public record ResumeProfileRequest(
        String name,
        String profileImageUrl,
        String phoneCountryCode,
        String phoneNumber,
        String introduction,
        List<TechStackRequest> techStacks,
        List<ExperienceRequest> experiences,
        List<EducationRequest> educations,
        List<ActivityRequest> activities,
        List<CertificateRequest> certificates) {

    public record TechStackRequest(String name) {}

    public record ExperienceRequest(
            Long id,
            String companyName,
            String position,
            String department,
            String startAt,
            String endAt,
            Boolean isCurrentlyWorking,
            String employmentType,
            String responsibilities) {}

    public record EducationRequest(
            Long id,
            String educationType,
            String institution,
            String major,
            String status,
            String startAt,
            String endAt) {}

    public record ActivityRequest(
            Long id, String title, String organization, Integer year, String description) {}

    public record CertificateRequest(
            Long id, String name, String score, String issuer, String issuedAt) {}
}
