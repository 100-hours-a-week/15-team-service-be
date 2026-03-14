package com.sipomeokjo.commitme.domain.worker.repository;

import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResumeVersionWorkerRepository {
    private final EntityManager entityManager;

    public Optional<ResumeVersion> findByIdWithPessimisticWrite(Long resumeVersionId) {
        if (resumeVersionId == null) {
            return Optional.empty();
        }

        java.util.List<ResumeVersion> result =
                entityManager
                        .createQuery(
                                "SELECT rv FROM ResumeVersion rv "
                                        + "JOIN FETCH rv.resume r "
                                        + "JOIN FETCH r.user "
                                        + "WHERE rv.id = :resumeVersionId",
                                ResumeVersion.class)
                        .setParameter("resumeVersionId", resumeVersionId)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .setMaxResults(1)
                        .getResultList();

        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.getFirst());
    }
}
