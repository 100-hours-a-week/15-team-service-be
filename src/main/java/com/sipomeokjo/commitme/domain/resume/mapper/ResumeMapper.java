package com.sipomeokjo.commitme.domain.resume.mapper;

import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeSummaryDto toSummaryDto(Resume resume) {
        if (resume == null) {
            return null;
        }

        RelatedInfo info = extractRelatedInfo(resume);

        return new ResumeSummaryDto(
                resume.getId(),
                resume.getName(),
                info.positionId(),
                info.positionName(),
                info.companyId(),
                info.companyName(),
                resume.getCurrentVersionNo(),
                resume.getUpdatedAt());
    }

    public ResumeDetailDto toDetailDto(Resume resume, ResumeVersion version, boolean isEditing) {
        if (resume == null || version == null) {
            return null;
        }

        RelatedInfo info = extractRelatedInfo(resume);

        return new ResumeDetailDto(
                resume.getId(),
                resume.getName(),
                isEditing,
                info.positionId(),
                info.positionName(),
                info.companyId(),
                info.companyName(),
                version.getVersionNo(),
                version.getContent(),
                version.getCommittedAt(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
    }

    private RelatedInfo extractRelatedInfo(Resume resume) {
        Long positionId = null;
        String positionName = null;
        if (resume.getPosition() != null) {
            positionId = resume.getPosition().getId();
            positionName = resume.getPosition().getName();
        }

        Long companyId = null;
        String companyName = null;
        if (resume.getCompany() != null) {
            companyId = resume.getCompany().getId();
            companyName = resume.getCompany().getName();
        }

        return new RelatedInfo(positionId, positionName, companyId, companyName);
    }

    private record RelatedInfo(
            Long positionId, String positionName, Long companyId, String companyName) {}
}
