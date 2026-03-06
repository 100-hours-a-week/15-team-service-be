package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.UserActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findAllByUser_IdOrderByIdAsc(Long userId);
}
