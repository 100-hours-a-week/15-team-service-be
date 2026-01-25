package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    Optional<ResumeVersion> findByResume_IdAndVersionNo(Long resumeId, Integer versionNo);

    void deleteByResume_Id(Long resumeId);
}
