package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    Optional<ResumeVersion> findByResume_IdAndVersionNo(Long resumeId, Integer versionNo);

    void deleteByResume_Id(Long resumeId);

    Optional<ResumeVersion> findByAiTaskId(String aiTaskId);

    Optional<ResumeVersion>
            findTopByResume_IdAndStatusAndCommittedAtIsNullAndPreviewShownAtIsNullOrderByVersionNoDesc(
                    Long resumeId, ResumeVersionStatus status);

    Optional<ResumeVersion> findTopByResume_IdAndStatusOrderByVersionNoDesc(
            Long resumeId, ResumeVersionStatus status);

    boolean existsByResume_IdAndStatusIn(Long resumeId, List<ResumeVersionStatus> statuses);

    boolean existsByResume_User_IdAndStatusIn(Long userId, List<ResumeVersionStatus> statuses);

    default boolean existsByUserIdAndStatusIn(Long userId, List<ResumeVersionStatus> statuses) {
        return existsByResume_User_IdAndStatusIn(userId, statuses);
    }

    @Query(
            value =
                    "SELECT rv.version_no AS versionNo "
                            + "FROM resume_version rv "
                            + "WHERE rv.resume_id = :resumeId "
                            + "ORDER BY rv.version_no DESC "
                            + "LIMIT 1",
            nativeQuery = true)
    Optional<ResumeVersionNoView> findLatestVersionNoByResumeId(@Param("resumeId") Long resumeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT rv.id FROM ResumeVersion rv "
                    + "WHERE rv.resume.id = :resumeId "
                    + "AND rv.status IN :statuses")
    List<Long> findByResumeIdAndStatusInWithLock(
            @Param("resumeId") Long resumeId,
            @Param("statuses") List<ResumeVersionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rv FROM ResumeVersion rv WHERE rv.status IN :statuses")
    List<ResumeVersion> findEntitiesByStatusIn(
            @Param("statuses") List<ResumeVersionStatus> statuses);
}
