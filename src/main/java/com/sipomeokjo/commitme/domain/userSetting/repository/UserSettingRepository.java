package com.sipomeokjo.commitme.domain.userSetting.repository;

import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);
}
