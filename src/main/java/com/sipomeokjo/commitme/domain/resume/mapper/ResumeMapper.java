package com.sipomeokjo.commitme.domain.resume.mapper;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeProfileResponse;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeSummaryDto toSummaryDtoFromDocument(ResumeDocument doc) {
        if (doc == null) return null;
        return new ResumeSummaryDto(
                doc.getResumeId(),
                doc.getName(),
                doc.getPositionId(),
                doc.getPositionName(),
                doc.getCompanyId(),
                doc.getCompanyName(),
                doc.getCurrentVersionNo(),
                doc.isHasPendingWork(),
                doc.getUpdatedAt());
    }

    public ResumeDetailDto toDetailDtoFromDocument(
            ResumeDocument doc,
            ResumeEventDocument event,
            boolean isEditing,
            ResumeProfileResponse profileResponse) {
        if (doc == null || event == null) return null;
        return new ResumeDetailDto(
                doc.getResumeId(),
                doc.getName(),
                isEditing,
                doc.getPositionId(),
                doc.getPositionName(),
                doc.getCompanyId(),
                doc.getCompanyName(),
                event.getVersionNo(),
                event.getSnapshot(),
                ResumeDetailDto.ResumeDetailProfileDto.from(profileResponse),
                event.getCommittedAt(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
