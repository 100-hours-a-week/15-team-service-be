package com.sipomeokjo.commitme.domain.notification.repository;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdAndIdGreaterThanOrderByIdAsc(Long userId, Long id);

    List<Notification> findByUser_IdOrderByIdDesc(Long userId, Pageable pageable);

    List<Notification> findByUser_IdAndIdLessThanOrderByIdDesc(
            Long userId, Long cursorId, Pageable pageable);

    Optional<Notification> findTopByUser_IdOrderByIdDesc(Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query(
            "update Notification n set n.readAt = :readAt"
                    + " where n.id = :id and n.user.id = :userId and n.readAt is null")
    int markRead(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("readAt") java.time.Instant readAt);
}
