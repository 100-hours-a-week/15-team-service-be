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
	
	Optional<ResumeVersion> findTopByResume_IdAndStatusOrderByVersionNoDesc(
			Long resumeId, ResumeVersionStatus status);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT rv FROM ResumeVersion rv " +
			"WHERE rv.resume.user.id = :userId " +
			"AND rv.status IN :statuses")
	List<ResumeVersion> findByUserIdAndStatusIn(
			@Param("userId") Long userId,
			@Param("statuses") List<ResumeVersionStatus> statuses);
}
