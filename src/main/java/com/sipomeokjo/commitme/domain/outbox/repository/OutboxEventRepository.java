package com.sipomeokjo.commitme.domain.outbox.repository;

import com.sipomeokjo.commitme.domain.outbox.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value =
                    "SELECT * FROM outbox_event oe "
                            + "WHERE oe.status IN (:statuses) "
                            + "AND oe.next_attempt_at <= :now "
                            + "ORDER BY oe.id ASC "
                            + "LIMIT :limit "
                            + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> findReadyEventsWithLock(
            @Param("statuses") List<String> statuses,
            @Param("now") Instant now,
            @Param("limit") int limit);
}
