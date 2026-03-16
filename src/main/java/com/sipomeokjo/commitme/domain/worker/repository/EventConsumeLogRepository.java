package com.sipomeokjo.commitme.domain.worker.repository;

import com.sipomeokjo.commitme.domain.worker.entity.EventConsumeLog;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventConsumeLogRepository extends JpaRepository<EventConsumeLog, Long> {

    Optional<EventConsumeLog> findByConsumerAndEventId(String consumer, String eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    INSERT IGNORE INTO event_consume_log
                        (created_at, updated_at, consumer, event_id, queue_name, status, lease_expires_at, processed_at)
                    VALUES
                        (:now, :now, :consumer, :eventId, :queueName, 'PROCESSING', :leaseExpiresAt, NULL)
                    """,
            nativeQuery = true)
    int insertProcessingIgnore(
            @Param("consumer") String consumer,
            @Param("eventId") String eventId,
            @Param("queueName") String queueName,
            @Param("now") Instant now,
            @Param("leaseExpiresAt") Instant leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE event_consume_log
                       SET queue_name = :queueName,
                           status = 'PROCESSING',
                           lease_expires_at = :leaseExpiresAt,
                           processed_at = NULL,
                           updated_at = :updatedAt
                     WHERE consumer = :consumer
                       AND event_id = :eventId
                       AND status = 'PROCESSING'
                       AND lease_expires_at IS NOT NULL
                       AND lease_expires_at <= :now
                    """,
            nativeQuery = true)
    int reclaimExpiredProcessing(
            @Param("consumer") String consumer,
            @Param("eventId") String eventId,
            @Param("queueName") String queueName,
            @Param("now") Instant now,
            @Param("updatedAt") Instant updatedAt,
            @Param("leaseExpiresAt") Instant leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value =
                    """
                    UPDATE event_consume_log
                       SET status = 'SUCCESS',
                           lease_expires_at = NULL,
                           processed_at = :processedAt,
                           updated_at = :processedAt
                     WHERE consumer = :consumer
                       AND event_id = :eventId
                       AND status = 'PROCESSING'
                    """,
            nativeQuery = true)
    int markSuccess(
            @Param("consumer") String consumer,
            @Param("eventId") String eventId,
            @Param("processedAt") Instant processedAt);

    void deleteByConsumerAndEventId(String consumer, String eventId);
}
