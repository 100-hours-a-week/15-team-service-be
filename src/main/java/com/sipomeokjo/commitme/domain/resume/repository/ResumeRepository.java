package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Page<Resume> findByUser_Id(Long userId, Pageable pageable);

    Optional<Resume> findByIdAndUser_Id(Long resumeId, Long userId);

    /**
     * SUCCEEDED 상태의 버전이 있는 이력서만 조회
     */
    @Query("SELECT DISTINCT r FROM Resume r " +
           "JOIN ResumeVersion rv ON rv.resume = r " +
           "WHERE r.user.id = :userId AND rv.status = 'SUCCEEDED'")
    Page<Resume> findSucceededByUserId(@Param("userId") Long userId, Pageable pageable);
}
