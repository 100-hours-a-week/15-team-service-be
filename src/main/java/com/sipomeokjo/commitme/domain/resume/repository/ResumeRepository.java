package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Page<Resume> findByUser_Id(Long userId, Pageable pageable);

    Optional<Resume> findByIdAndUser_Id(Long resumeId, Long userId);
}
