package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.ResumeProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ResumeProfile rp WHERE rp.userId IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}
