package com.sipomeokjo.commitme.domain.interview.dto.ai;

import com.sipomeokjo.commitme.domain.interview.entity.InterviewType;
import java.util.List;

public record AiInterviewEndRequest(
        String aiSessionId,
        InterviewType interviewType,
        String position,
        String company,
        List<MessagePayload> messages,
        ProfilePayload profile) {

    public record MessagePayload(
            Integer turnNo,
            String question,
            String answer,
            String answerInputType,
            String askedAt,
            String answeredAt) {}

    public record ProfilePayload(
            String name,
            String profileImageUrl,
            String phoneCountryCode,
            String phoneNumber,
            String introduction,
            List<TechStackItem> techStacks,
            List<ExperienceItem> experiences,
            List<EducationItem> educations,
            List<ActivityItem> activities,
            List<CertificateItem> certificates) {

        public record TechStackItem(Long id, String name) {}

        public record ExperienceItem(
                Long id,
                String companyName,
                String position,
                String department,
                String startAt,
                String endAt,
                Boolean isCurrentlyWorking,
                String employmentType,
                String responsibilities) {}

        public record EducationItem(
                Long id,
                String educationType,
                String institution,
                String major,
                String status,
                String startAt,
                String endAt) {}

        public record ActivityItem(
                Long id, String title, String organization, Integer year, String description) {}

        public record CertificateItem(
                Long id, String name, String score, String issuer, String issuedAt) {}
    }
}
