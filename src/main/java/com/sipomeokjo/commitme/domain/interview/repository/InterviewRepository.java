package com.sipomeokjo.commitme.domain.interview.repository;

import com.sipomeokjo.commitme.domain.interview.entity.Interview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    @Query(
            "SELECT i FROM Interview i "
                    + "JOIN FETCH i.position "
                    + "LEFT JOIN FETCH i.company "
                    + "WHERE i.user.id = :userId "
                    + "ORDER BY i.createdAt DESC")
    List<Interview> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Query(
            "SELECT i FROM Interview i "
                    + "JOIN FETCH i.position "
                    + "LEFT JOIN FETCH i.company "
                    + "WHERE i.id = :id AND i.user.id = :userId")
    Optional<Interview> findByIdAndUserIdWithDetails(
            @Param("id") Long id, @Param("userId") Long userId);

    Optional<Interview> findByIdAndUserId(Long id, Long userId);

    Optional<Interview> findByAiSessionId(String aiSessionId);
}
