package com.sipomeokjo.commitme.domain.notification.repository;

import com.sipomeokjo.commitme.domain.notification.entity.NotificationSeen;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSeenRepository
        extends org.springframework.data.repository.Repository<NotificationSeen, Long> {

    @Query("select ns.lastSeenId from NotificationSeen ns where ns.userId = :userId")
    Long findLastSeenIdByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO notification_seen (user_id, last_seen_id, updated_at)
                    VALUES (:userId, :lastSeenId, :updatedAt) AS new_seen
                    ON DUPLICATE KEY UPDATE
                        last_seen_id = GREATEST(notification_seen.last_seen_id, new_seen.last_seen_id),
                        updated_at = new_seen.updated_at
                    """,
            nativeQuery = true)
    int upsertSeen(
            @Param("userId") Long userId,
            @Param("lastSeenId") Long lastSeenId,
            @Param("updatedAt") Instant updatedAt);
}
