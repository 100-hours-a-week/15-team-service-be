package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventAggregationResult.VersionListResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

/**
 * MongoDB Aggregation Pipeline 기반 이력서 이벤트 복합 조회.
 *
 * <p>두 결과가 동일한 outer $match 조건을 공유하는 경우에만 $facet을 적용한다.
 */
@Repository
@RequiredArgsConstructor
public class ResumeEventAggregationRepository {

    private final MongoTemplate mongoTemplate;

    /**
     * 버전 목록 조회용 aggregation.
     *
     * <p>커밋된 SUCCEEDED 이벤트를 커서 기반으로 페이지네이션하고, 첫 페이지에서는 최신 SUCCEEDED 이벤트(미커밋 포함)도 함께 반환한다.
     *
     * <p>Pipeline: $match(resume_id, status=SUCCEEDED) → $facet(committedPage, latestSucceeded)
     *
     * @param resumeId 이력서 ID
     * @param cursorVersionNo 커서 버전 번호 (null이면 첫 페이지)
     * @param pageSize 페이지 크기 (hasMore 판단을 위해 실제로 pageSize+1 조회)
     */
    public VersionListResult findVersionListPage(
            Long resumeId, Integer cursorVersionNo, int pageSize) {

        MatchOperation outerMatch =
                Aggregation.match(
                        Criteria.where("resume_id")
                                .is(resumeId)
                                .and("status")
                                .is(ResumeVersionStatus.SUCCEEDED));

        List<AggregationOperation> committedBranch = new ArrayList<>();
        Criteria committedCriteria = Criteria.where("committed_at").ne(null);
        if (cursorVersionNo != null) {
            committedCriteria = committedCriteria.and("version_no").lt(cursorVersionNo);
        }
        committedBranch.add(Aggregation.match(committedCriteria));
        committedBranch.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "version_no")));
        committedBranch.add(Aggregation.limit(pageSize + 1L));

        List<AggregationOperation> latestBranch =
                List.of(
                        Aggregation.sort(Sort.by(Sort.Direction.DESC, "version_no")),
                        Aggregation.limit(1L));

        FacetOperation facet =
                Aggregation.facet(committedBranch.toArray(new AggregationOperation[0]))
                        .as("committedPage")
                        .and(latestBranch.toArray(new AggregationOperation[0]))
                        .as("latestSucceeded");

        Aggregation agg = Aggregation.newAggregation(ResumeEventDocument.class, outerMatch, facet);
        AggregationResults<Document> results =
                mongoTemplate.aggregate(agg, ResumeEventDocument.class, Document.class);

        Document output = results.getUniqueMappedResult();
        if (output == null) {
            return new VersionListResult(List.of(), null);
        }

        MongoConverter converter = mongoTemplate.getConverter();

        List<ResumeEventDocument> committedPage =
                output.getList("committedPage", Document.class).stream()
                        .map(d -> converter.read(ResumeEventDocument.class, d))
                        .toList();

        List<Document> latestDocs = output.getList("latestSucceeded", Document.class);
        ResumeEventDocument latestSucceeded =
                latestDocs.isEmpty()
                        ? null
                        : converter.read(ResumeEventDocument.class, latestDocs.getFirst());

        return new VersionListResult(committedPage, latestSucceeded);
    }
}
