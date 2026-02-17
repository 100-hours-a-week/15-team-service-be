package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndUser_Id(Long resumeId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resume r WHERE r.id = :resumeId AND r.user.id = :userId")
    Optional<Resume> findByIdAndUserIdWithLock(
            @Param("resumeId") Long resumeId, @Param("userId") Long userId);

    @Query(
            "SELECT DISTINCT r FROM Resume r "
                    + "JOIN ResumeVersion rv ON rv.resume = r "
                    + "WHERE r.user.id = :userId AND rv.status = 'SUCCEEDED' "
                    + "AND (:keyword is null OR r.name LIKE CONCAT('%', :keyword, '%')) "
                    + "AND (:cursorUpdatedAt is null "
                    + "OR r.updatedAt > :cursorUpdatedAt "
                    + "OR (r.updatedAt = :cursorUpdatedAt AND r.id > :cursorId)) "
                    + "ORDER BY r.updatedAt ASC, r.id ASC")
    List<Resume> findSucceededByUserIdWithCursorAsc(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorUpdatedAt") Instant cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(
            "SELECT DISTINCT r FROM Resume r "
                    + "JOIN ResumeVersion rv ON rv.resume = r "
                    + "WHERE r.user.id = :userId AND rv.status = 'SUCCEEDED' "
                    + "AND (:keyword is null OR r.name LIKE CONCAT('%', :keyword, '%')) "
                    + "AND (:cursorUpdatedAt is null "
                    + "OR r.updatedAt < :cursorUpdatedAt "
                    + "OR (r.updatedAt = :cursorUpdatedAt AND r.id < :cursorId)) "
                    + "ORDER BY r.updatedAt DESC, r.id DESC")
    List<Resume> findSucceededByUserIdWithCursorDesc(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorUpdatedAt") Instant cursorUpdatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);
}
