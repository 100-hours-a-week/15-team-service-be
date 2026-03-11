package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.UserExperience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExperienceRepository extends JpaRepository<UserExperience, Long> {
    List<UserExperience> findAllByUser_IdOrderByCreatedAtAsc(Long userId);
}
