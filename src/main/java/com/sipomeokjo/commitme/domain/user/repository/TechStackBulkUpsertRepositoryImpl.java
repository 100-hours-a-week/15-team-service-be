package com.sipomeokjo.commitme.domain.user.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import org.springframework.stereotype.Repository;

@Repository
public class TechStackBulkUpsertRepositoryImpl implements TechStackBulkUpsertRepository {

    @PersistenceContext private EntityManager entityManager;

    @Override
    public void upsertAll(Collection<TechStackUpsertRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        Timestamp nowTimestamp = Timestamp.from(now);
        StringBuilder sql =
                new StringBuilder(
                        """
                        INSERT INTO tech_stacks (created_at, name, name_normalized, updated_at)
                        VALUES
                        """);

        int index = 0;
        for (TechStackUpsertRow ignored : rows) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append(
                    String.format(
                            "(:createdAt%d, :name%d, :nameNormalized%d, :updatedAt%d)",
                            index, index, index, index));
            index++;
        }

        sql.append(
                """
                 AS new_stack
                ON DUPLICATE KEY UPDATE
                    updated_at = new_stack.updated_at
                """);

        var query = entityManager.createNativeQuery(sql.toString());

        index = 0;
        for (TechStackUpsertRow row : rows) {
            query.setParameter("createdAt" + index, nowTimestamp);
            query.setParameter("name" + index, row.name());
            query.setParameter("nameNormalized" + index, row.nameNormalized());
            query.setParameter("updatedAt" + index, nowTimestamp);
            index++;
        }

        query.executeUpdate();
    }
}
