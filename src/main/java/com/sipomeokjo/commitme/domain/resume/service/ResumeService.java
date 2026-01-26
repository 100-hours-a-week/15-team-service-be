package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.PageResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.entity.AuthProvider;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import com.sipomeokjo.commitme.domain.resume.dto.*;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ai.AiResumeGenerateResponse;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.security.AccessTokenCipher;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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

    // AI 연동에 필요 요소
    private final RestClient aiClient;
    private final AiProperties aiProperties;
    private final com.sipomeokjo.commitme.domain.auth.repository.AuthRepository authRepository;
    private final AccessTokenCipher accessTokenCipher;

    // 이력서 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<ResumeSummaryDto> list(Long userId, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Resume> result = resumeRepository.findByUser_Id(userId, pageable);

        List<Resume> resumes = result.getContent();
        List<ResumeSummaryDto> items = new ArrayList<>(resumes.size());

        for (Resume r : resumes) {
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

            items.add(new ResumeSummaryDto(
                    r.getId(),
                    r.getName(),
                    positionId,
                    positionName,
                    companyId,
                    companyName,
                    r.getCurrentVersionNo(),
                    r.getUpdatedAt()
            ));
        }

        PageResponse.PageMeta meta = new PageResponse.PageMeta(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        return new PageResponse<>(items, meta);
    }

    // 이력서 생성 + AI 생성 요청
    public Long create(Long userId, ResumeCreateRequest req) {

        // 0) AI 명세서 기준: repoUrls 필수
        if (req.getRepoUrls() == null || req.getRepoUrls().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST); // 프로젝트 에러코드에 맞게 변경 가능
        }

        // 1) name 정책
        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty()) name = "새 이력서";
        if (name.length() > 18) throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);

        // 2) position 필수 + 존재 검증
        if (req.getPositionId() == null) throw new BusinessException(ErrorCode.POSITION_SELECTION_REQUIRED);
        Position position = positionRepository.findById(req.getPositionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));

        // 3) company 선택 + 존재 검증
        Company company = null;
        if (req.getCompanyId() != null) {
            company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        }

        // 4) user reference
        User userRef = em.getReference(User.class, userId);

        // 5) resume 저장
        Resume resume = Resume.create(userRef, position, company, name);
        Resume saved = resumeRepository.save(resume);

        // 6) v1 생성 (JSON 컬럼 안전: "{}")
        // createV1이 SUCCEEDED로 박혀있더라도 아래에서 markQueued로 덮어쓸 거라 일단 "{}"로 생성
        ResumeVersion v1 = ResumeVersion.createV1(saved, "{}");
        v1.markQueued();
        resumeVersionRepository.save(v1);

        // ===== AI generate 요청 =====
        // (1) github token 가져오기 (Auth 저장 시 encrypt 했으므로 decrypt 필요)
        var auth = authRepository.findByUser_IdAndProvider(userId, AuthProvider.GITHUB)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        String githubToken = accessTokenCipher.decrypt(auth.getAccessToken());

        // (2) position 문자열 (AI 명세가 enum 문자열이면 여기서 매핑 필요)
        // 지금은 일단 그대로 사용. 필요하면 아래 메서드에서 매핑해.
        String positionForAi = toAiPosition(position.getName());

        // (3) 요청 데이터 만들기 (AI 명세서 기준)
        AiResumeGenerateRequest aiReq = new AiResumeGenerateRequest(
                req.getRepoUrls(),
                positionForAi,
                githubToken,
                aiProperties.getResumeCallbackUrl()
        );

        try {
            // ✅ baseUrl/path 혼동 방지: 여기서 합쳐서 호출
            String url = aiProperties.getBaseUrl() + aiProperties.getResumeGeneratePath();

            AiResumeGenerateResponse aiRes = aiClient.post()
                    .uri(url)
                    .body(aiReq)
                    .retrieve()
                    .body(AiResumeGenerateResponse.class);

            if (aiRes == null || aiRes.jobId() == null || aiRes.jobId().isBlank()) {
                v1.failNow("AI_RESPONSE_INVALID", "jobId is null/blank");
                return saved.getId();
            }

            v1.startProcessing(aiRes.jobId());

        } catch (Exception e) {
            v1.failNow("AI_GENERATE_FAILED", e.getMessage());
        }

        return saved.getId();
    }

    /**
     * AI 명세가 "backend", "frontend" 같은 고정 문자열이면 여기서 매핑해.
     * 지금은 일단 그대로 반환.
     */
    private String toAiPosition(String positionName) {
        return positionName;
    }

    // 특정 이력서 조회
    @Transactional(readOnly = true)
    public ResumeDetailDto get(Long userId, Long resumeId) {

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        Integer currentVersionNo = resume.getCurrentVersionNo();
        if (currentVersionNo == null) throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND);

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
        if (name.isEmpty() || name.length() > 18) throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resume.rename(name);
    }

    // 이력서 저장 (versionNo를 current로 변경 + committedAt 기록)
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
