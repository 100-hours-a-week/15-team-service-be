package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResumeEventQueryRepository {
    private final MongoTemplate mongoTemplate;

    public Optional<ResumeEventDocument> transitionToTerminalIfPossible(
            String aiTaskId,
            ResumeVersionStatus target,
            String snapshot,
            String errorLog,
            Instant finishedAt,
            Instant committedAt) {
        Criteria criteria =
                Criteria.where("ai_task_id")
                        .is(aiTaskId)
                        .and("status")
                        .nin(ResumeVersionStatus.SUCCEEDED, ResumeVersionStatus.FAILED);

        Update update =
                new Update()
                        .set("status", target)
                        .set("is_pending", false)
                        .set("finished_at", finishedAt)
                        .set("updated_at", finishedAt);
        if (snapshot != null) update.set("snapshot", snapshot);
        if (errorLog != null) update.set("error_log", errorLog);
        if (committedAt != null) update.set("committed_at", committedAt);

        ResumeEventDocument result =
                mongoTemplate.findAndModify(
                        Query.query(criteria),
                        update,
                        FindAndModifyOptions.options().returnNew(true),
                        ResumeEventDocument.class);
        return Optional.ofNullable(result);
    }
}
