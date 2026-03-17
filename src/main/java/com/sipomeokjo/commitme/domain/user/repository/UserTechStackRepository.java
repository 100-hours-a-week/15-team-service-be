package com.sipomeokjo.commitme.domain.user.repository;

import com.sipomeokjo.commitme.domain.user.entity.UserTechStack;
import com.sipomeokjo.commitme.domain.user.entity.UserTechStackId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, UserTechStackId> {
    void deleteAllByUser_Id(Long userId);

    List<UserTechStack> findAllByUser_Id(Long userId);
}
