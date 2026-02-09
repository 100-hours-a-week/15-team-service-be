package com.sipomeokjo.commitme.domain.resume.mapper;

import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeSummaryDto toSummaryDto(Resume resume) {
        if (resume == null) {
            return null;
        }

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

        return new ResumeSummaryDto(
                resume.getId(),
                resume.getName(),
                positionId,
                positionName,
                companyId,
                companyName,
                resume.getCurrentVersionNo(),
                resume.getUpdatedAt());
    }
}
