package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResumeMongoQueryRepository {

    private final MongoTemplate mongoTemplate;

    public List<ResumeDocument> findByUserIdWithCursorDesc(
            Long userId, String keyword, Instant cursorUpdatedAt, Long cursorResumeId, int limit) {
        Criteria criteria = buildBaseCriteria(userId, keyword);
        if (cursorUpdatedAt != null && cursorResumeId != null) {
            criteria =
                    criteria.andOperator(
                            new Criteria()
                                    .orOperator(
                                            Criteria.where("updated_at").lt(cursorUpdatedAt),
                                            new Criteria()
                                                    .andOperator(
                                                            Criteria.where("updated_at")
                                                                    .is(cursorUpdatedAt),
                                                            Criteria.where("resume_id")
                                                                    .lt(cursorResumeId))));
        }
        Query query =
                Query.query(criteria)
                        .with(Sort.by(Sort.Direction.DESC, "updated_at", "resume_id"))
                        .limit(limit);
        return mongoTemplate.find(query, ResumeDocument.class);
    }

    public List<ResumeDocument> findByUserIdWithCursorAsc(
            Long userId, String keyword, Instant cursorUpdatedAt, Long cursorResumeId, int limit) {
        Criteria criteria = buildBaseCriteria(userId, keyword);
        if (cursorUpdatedAt != null && cursorResumeId != null) {
            criteria =
                    criteria.andOperator(
                            new Criteria()
                                    .orOperator(
                                            Criteria.where("updated_at").gt(cursorUpdatedAt),
                                            new Criteria()
                                                    .andOperator(
                                                            Criteria.where("updated_at")
                                                                    .is(cursorUpdatedAt),
                                                            Criteria.where("resume_id")
                                                                    .gt(cursorResumeId))));
        }
        Query query =
                Query.query(criteria)
                        .with(Sort.by(Sort.Direction.ASC, "updated_at", "resume_id"))
                        .limit(limit);
        return mongoTemplate.find(query, ResumeDocument.class);
    }

    public void clearUnseenPreviewIfPresent(Long resumeId) {
        Query query =
                Query.query(
                        Criteria.where("resume_id")
                                .is(resumeId)
                                .and("has_unseen_preview")
                                .is(true));
        Update update = Update.update("has_unseen_preview", false);
        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                ResumeDocument.class);
    }

    public List<Long> findAllResumeIdsBatch(int skip, int limit) {
        Query query =
                new Query().skip(skip).limit(limit).with(Sort.by(Sort.Direction.ASC, "resume_id"));
        query.fields().include("resume_id");
        return mongoTemplate.find(query, ResumeDocument.class).stream()
                .map(ResumeDocument::getResumeId)
                .toList();
    }

    private Criteria buildBaseCriteria(Long userId, String keyword) {
        Criteria criteria =
                Criteria.where("user_id").is(userId).and("latest_succeeded_version_no").ne(null);
        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and("name").regex(Pattern.quote(keyword), "i");
        }
        return criteria;
    }
}
