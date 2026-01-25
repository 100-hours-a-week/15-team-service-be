package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.PageResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;

    private final PositionRepository positionRepository;
    private final CompanyRepository companyRepository;

    private final EntityManager em;

    // 이력서 목록 조회 (페이지만 페이지네이션)
    @Transactional(readOnly = true)
    public PageResponse<ResumeSummaryDto> list(Long userId, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Resume> result = resumeRepository.findByUser_Id(userId, pageable);

        List<Resume> resumes = result.getContent();
        List<ResumeSummaryDto> items = new ArrayList<ResumeSummaryDto>(resumes.size());

        for (int i = 0; i < resumes.size(); i++) {
            Resume r = resumes.get(i);

            Long positionId = null;
            String positionName = null;
            if (r.getPosition() != null) {
                positionId = r.getPosition().getId();
                positionName = r.getPosition().getName();
            }

            Long companyId = null;
            String companyName = null;
            if (r.getCompany() != null) {
                companyId = r.getCompany().getId();
                companyName = r.getCompany().getName();
            }

            ResumeSummaryDto dto = new ResumeSummaryDto(
                    r.getId(),
                    r.getName(),
                    positionId,
                    positionName,
                    companyId,
                    companyName,
                    r.getCurrentVersionNo(),
                    r.getUpdatedAt()
            );
            items.add(dto);
        }

        PageResponse.PageMeta meta = new PageResponse.PageMeta(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return new PageResponse<ResumeSummaryDto>(items, meta);
    }

    // 이력서 생성 (position/company 존재 검증 포함)
    public Long create(Long userId, ResumeCreateRequest req) {

        // 1) name 정책
        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty()) {
            name = "새 이력서";
        }
        if (name.length() > 18) {
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);
        }

        // 2) position 필수 + 존재 검증
        if (req.getPositionId() == null) {
            throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
        }
        Position position = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));

        // 3) company 선택 + 존재 검증
        Company company = null;
        if (req.getCompanyId() != null) {
            company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        // 4) user는 reference로 충분 (인증 붙이면 userId는 보장된 값)
        User userRef = em.getReference(User.class, userId);

        // 5) 엔티티 도메인 메서드로 생성 (protected 생성자 문제 해결)
        Resume resume = Resume.create(userRef, position, company, name);
        Resume saved = resumeRepository.save(resume);

        // 6) v1 버전 생성 (엔티티 도메인 메서드로 생성)
        ResumeVersion v1 = ResumeVersion.createV1(saved, "");
        resumeVersionRepository.save(v1);

        return saved.getId();
    }

    // 특정 이력서 조회
    @Transactional(readOnly = true)
    public ResumeDetailDto get(Long userId, Long resumeId) {

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        Integer currentVersionNo = resume.getCurrentVersionNo();
        if (currentVersionNo == null) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND);
        }

        ResumeVersion version = resumeVersionRepository.findByResume_IdAndVersionNo(resume.getId(), currentVersionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

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

        return new ResumeDetailDto(
                resume.getId(),
                resume.getName(),
                positionId,
                positionName,
                companyId,
                companyName,
                resume.getCurrentVersionNo(),
                version.getContent(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }

    // 특정 버전 조회
    @Transactional(readOnly = true)
    public ResumeVersionDto getVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion v = resumeVersionRepository.findByResume_IdAndVersionNo(resume.getId(), versionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        return new ResumeVersionDto(
                resume.getId(),
                v.getVersionNo(),
                v.getStatus(),
                v.getContent(),
                v.getAiTaskId(),
                v.getErrorLog(),
                v.getStartedAt(),
                v.getFinishedAt(),
                v.getCommittedAt(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }

    // 이름 수정
    public void rename(Long userId, Long resumeId, ResumeRenameRequest req) {

        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 18) {
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);
        }

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resume.rename(name);
    }

    // 이력서 저장 (특정 version를 current로 변경 + committedAt 기록)
    public void saveVersion(Long userId, Long resumeId, int versionNo) {

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion v = resumeVersionRepository.findByResume_IdAndVersionNo(resume.getId(), versionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (v.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        v.commitNow();
        resume.setCurrentVersionNo(versionNo);
    }

    // 삭제
    public void delete(Long userId, Long resumeId) {

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resumeVersionRepository.deleteByResume_Id(resume.getId());
        resumeRepository.delete(resume);
    }
}
