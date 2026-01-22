package com.sipomeokjo.commitme.domain.resume.repository;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    Optional<ResumeVersion> findByResume_IdAndVersionNo(Long resumeId, Integer versionNo);

    void deleteByResume_Id(Long resumeId);
}
