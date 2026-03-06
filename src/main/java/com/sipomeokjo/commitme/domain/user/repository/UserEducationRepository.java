package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.UserEducation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEducationRepository extends JpaRepository<UserEducation, Long> {
    List<UserEducation> findAllByUser_IdOrderByIdAsc(Long userId);
}
