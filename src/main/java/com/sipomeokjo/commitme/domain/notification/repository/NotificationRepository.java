package com.sipomeokjo.commitme.domain.notification.repository;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdAndIdGreaterThanOrderByIdAsc(Long userId, Long id);
}
