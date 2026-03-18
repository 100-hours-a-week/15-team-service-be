package com.sipomeokjo.commitme.domain.resume.mapper;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeSummaryDto toSummaryDto(Resume resume, boolean isEditing) {
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
                isEditing,
                resume.getUpdatedAt());
    }

    public ResumeDetailDto toDetailDto(
            Resume resume,
            ResumeEventDocument event,
            boolean isEditing,
            ResumeProfileResponse profileResponse) {
        if (resume == null || event == null) return null;

        RelatedInfo info = extractRelatedInfo(resume);

        return new ResumeDetailDto(
                resume.getId(),
                resume.getName(),
                isEditing,
                info.positionId(),
                info.positionName(),
                info.companyId(),
                info.companyName(),
                event.getVersionNo(),
                event.getSnapshot(),
                ResumeDetailDto.ResumeDetailProfileDto.from(profileResponse),
                event.getCommittedAt(),
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
