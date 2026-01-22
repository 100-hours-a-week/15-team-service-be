package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.pagination.PageResponse;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeCreateRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeDetailDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeRenameRequest;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeSummaryDto;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeVersionDto;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeVersionRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    public PageResponse<ResumeSummaryDto> list(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Resume> result = resumeRepository.findByUser_Id(userId, pageable);

        List<Resume> resumes = result.getContent();
        List<ResumeSummaryDto> items = new ArrayList<>(resumes.size());

        for (int i = 0; i < resumes.size(); i++) {
            Resume r = resumes.get(i);

            Long positionId = (r.getPosition() == null) ? null : r.getPosition().getId();
            String positionName = (r.getPosition() == null) ? null : r.getPosition().getName();

            Long companyId = (r.getCompany() == null) ? null : r.getCompany().getId();
            String companyName = (r.getCompany() == null) ? null : r.getCompany().getName();

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

        return new PageResponse<>(items, meta);
    }

    public Long create(Long userId, ResumeCreateRequest req) {
        String name = (req.getName() == null || req.getName().trim().isEmpty()) ? "새 이력서" : req.getName().trim();
        if (name.length() > 18) { // 너가 말한 정책 기준
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);
        }

        if (req.getPositionId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        User userRef = em.getReference(User.class, userId);
        Position positionRef = em.getReference(Position.class, req.getPositionId());
        Company companyRef = (req.getCompanyId() == null) ? null : em.getReference(Company.class, req.getCompanyId());

        // 엔티티 필드가 private + setter 없음이라, "생성자/팩토리"가 엔티티에 없는 경우 여기서 막힘.
        // 따라서 아래는 "엔티티에 최소 생성 메서드(팩토리) 추가"를 전제로 함.
        // (필드 변경 없이 메서드만 추가하는 건 ERD 확정과 충돌 안 함)
        Resume resume = ResumeFactory.create(userRef, positionRef, companyRef, name);

        Resume saved = resumeRepository.save(resume);

        ResumeVersion v1 = ResumeVersionFactory.createV1(saved, "");
        resumeVersionRepository.save(v1);

        // current_version_no = 1 세팅도 ResumeFactory에서 함
        return saved.getId();
    }

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

        Long positionId = (resume.getPosition() == null) ? null : resume.getPosition().getId();
        String positionName = (resume.getPosition() == null) ? null : resume.getPosition().getName();

        Long companyId = (resume.getCompany() == null) ? null : resume.getCompany().getId();
        String companyName = (resume.getCompany() == null) ? null : resume.getCompany().getName();

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

    public void rename(Long userId, Long resumeId, ResumeRenameRequest req) {
        String name = (req.getName() == null) ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 18) {
            throw new BusinessException(ErrorCode.INVALID_RESUME_NAME);
        }

        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeFactory.rename(resume, name);
    }

    public void saveVersion(Long userId, Long resumeId, int versionNo) {
        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        ResumeVersion v = resumeVersionRepository.findByResume_IdAndVersionNo(resume.getId(), versionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_VERSION_NOT_FOUND));

        if (v.getStatus() != ResumeVersionStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.RESUME_VERSION_NOT_READY);
        }

        ResumeVersionFactory.commitNow(v);
        ResumeFactory.setCurrentVersionNo(resume, versionNo);
    }

    public void delete(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));

        resumeVersionRepository.deleteByResume_Id(resume.getId());
        resumeRepository.delete(resume);
    }

    /**
     * ✅ 엔티티 "필드 구조"는 그대로 두고, 생성/수정만 가능하게 해주는 최소 유틸.
     * 실제 프로젝트에선 Resume 엔티티 내부에 정적 팩토리/도메인 메서드로 옮기는 걸 추천.
     */
    static class ResumeFactory {
        static Resume create(User user, Position position, Company company, String name) {
            Resume r = new Resume();
            // protected 생성자라 같은 패키지/내부클래스에서만 new가 가능할 수 있음.
            // 만약 여기서 접근이 막히면 Resume 엔티티에 "static create(...)" 메서드 하나만 추가하면 해결됨.

            ReflectionSetter.set(r, "user", user);
            ReflectionSetter.set(r, "position", position);
            ReflectionSetter.set(r, "company", company);
            ReflectionSetter.set(r, "name", name);
            ReflectionSetter.set(r, "currentVersionNo", 1);
            ReflectionSetter.set(r, "activeVersionNo", null);
            return r;
        }

        static void rename(Resume resume, String name) {
            ReflectionSetter.set(resume, "name", name);
        }

        static void setCurrentVersionNo(Resume resume, int versionNo) {
            ReflectionSetter.set(resume, "currentVersionNo", versionNo);
        }
    }

    static class ResumeVersionFactory {
        static ResumeVersion createV1(Resume resume, String content) {
            ResumeVersion v = new ResumeVersion();

            ReflectionSetter.set(v, "resume", resume);
            ReflectionSetter.set(v, "versionNo", 1);
            ReflectionSetter.set(v, "status", ResumeVersionStatus.SUCCEEDED);
            ReflectionSetter.set(v, "content", (content == null) ? "" : content);

            ReflectionSetter.set(v, "aiTaskId", null);
            ReflectionSetter.set(v, "errorLog", null);
            ReflectionSetter.set(v, "startedAt", LocalDateTime.now());
            ReflectionSetter.set(v, "finishedAt", LocalDateTime.now());
            ReflectionSetter.set(v, "committedAt", null);

            return v;
        }

        static void commitNow(ResumeVersion v) {
            ReflectionSetter.set(v, "committedAt", LocalDateTime.now());
        }
    }

    /**
     * ⚠️ 임시방편: 엔티티에 setter/팩토리 메서드 추가하기 싫을 때만.
     * 지금 단계에서 "엔티티는 확정"이라 해서 필드 유지하면서 서비스만 맞추려고 넣었음.
     * 가능하면 Resume/ResumeVersion 엔티티에 create/rename/commitNow 같은 메서드 3~4개만 추가하고
     * 이 ReflectionSetter는 삭제하는 게 베스트.
     */
    static class ReflectionSetter {
        static void set(Object target, String fieldName, Object value) {
            try {
                java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to set field: " + fieldName + " on " + target.getClass().getSimpleName(), e);
            }
        }
    }
}
