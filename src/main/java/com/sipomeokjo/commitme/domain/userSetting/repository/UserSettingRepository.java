package com.sipomeokjo.commitme.domain.userSetting.repository;

import com.sipomeokjo.commitme.domain.userSetting.entity.UserSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);

    @Modifying
    @Query("delete from UserSetting us where us.user.id in :userIds")
    int deleteAllByUserIds(@Param("userIds") java.util.List<Long> userIds);
}
