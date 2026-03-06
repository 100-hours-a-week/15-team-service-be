package com.sipomeokjo.commitme.domain.resume.mapper;

import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.user.entity.UserActivity;
import com.sipomeokjo.commitme.domain.user.entity.UserCertificate;
import com.sipomeokjo.commitme.domain.user.entity.UserEducation;
import com.sipomeokjo.commitme.domain.user.entity.UserExperience;
import com.sipomeokjo.commitme.domain.user.entity.UserTechStack;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResumeProfileMapper {

    public List<ResumeProfileResponse.TechStackResponse> toTechStackResponses(
            List<UserTechStack> techStacks) {
        return techStacks.stream().map(this::toTechStackResponse).toList();
    }

    public List<ResumeProfileResponse.ExperienceResponse> toExperienceResponses(
            List<UserExperience> experiences) {
        return experiences.stream().map(this::toExperienceResponse).toList();
    }

    public List<ResumeProfileResponse.EducationResponse> toEducationResponses(
            List<UserEducation> educations) {
        return educations.stream().map(this::toEducationResponse).toList();
    }

    public List<ResumeProfileResponse.ActivityResponse> toActivityResponses(
            List<UserActivity> activities) {
        return activities.stream().map(this::toActivityResponse).toList();
    }

    public List<ResumeProfileResponse.CertificateResponse> toCertificateResponses(
            List<UserCertificate> certificates) {
        return certificates.stream().map(this::toCertificateResponse).toList();
    }

    private ResumeProfileResponse.TechStackResponse toTechStackResponse(UserTechStack techStack) {
        return new ResumeProfileResponse.TechStackResponse(
                techStack.getTechStack().getId(), techStack.getTechStack().getName());
    }

    private ResumeProfileResponse.ExperienceResponse toExperienceResponse(
            UserExperience experience) {
        return new ResumeProfileResponse.ExperienceResponse(
                experience.getId(),
                experience.getCompanyName(),
                experience.getPositionTitle(),
                experience.getDepartmentName(),
                formatYearMonth(experience.getStartYear(), experience.getStartMonth()),
                formatYearMonth(experience.getEndYear(), experience.getEndMonth()),
                experience.isCurrent(),
                experience.getEmploymentType().name(),
                experience.getDescription());
    }

    private ResumeProfileResponse.EducationResponse toEducationResponse(UserEducation education) {
        return new ResumeProfileResponse.EducationResponse(
                education.getId(),
                education.getType().name(),
                education.getInstitution(),
                education.getMajorField(),
                education.getStatus().name(),
                formatYearMonth(education.getStartYear(), education.getStartMonth()),
                formatYearMonth(education.getEndYear(), education.getEndMonth()));
    }

    private ResumeProfileResponse.ActivityResponse toActivityResponse(UserActivity activity) {
        return new ResumeProfileResponse.ActivityResponse(
                activity.getId(),
                activity.getActivityName(),
                activity.getOrganization(),
                activity.getActivityYear() == null ? null : activity.getActivityYear().intValue(),
                activity.getDescription());
    }

    private ResumeProfileResponse.CertificateResponse toCertificateResponse(
            UserCertificate certificate) {
        return new ResumeProfileResponse.CertificateResponse(
                certificate.getId(),
                certificate.getTitle(),
                certificate.getGradeOrScore(),
                certificate.getIssuer(),
                formatYearMonth(certificate.getAcquiredYear(), certificate.getAcquiredMonth()));
    }

    private String formatYearMonth(Short year, Byte month) {
        if (year == null || month == null) {
            return null;
        }
        return String.format("%04d.%02d", year, month);
    }
}
