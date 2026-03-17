package com.sipomeokjo.commitme.domain.resume.repository.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.global.config.MongoConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataMongoTest
@ActiveProfiles("test")
@Import({MongoConfig.class, ResumeEventQueryRepository.class})
class ResumeEventMongoRepositoryTest {

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired private ResumeEventMongoRepository resumeEventMongoRepository;
    @Autowired private ResumeEventQueryRepository resumeEventQueryRepository;
    @Autowired private MongoTemplate mongoTemplate;

    @AfterEach
    void tearDown() {
        resumeEventMongoRepository.deleteAll();
    }

    @Test
    void indexes_createdForResumeEvents() {
        List<IndexInfo> indexInfos =
                mongoTemplate.indexOps(ResumeEventDocument.class).getIndexInfo();

        assertThat(indexInfos)
                .extracting(IndexInfo::getName)
                .contains(
                        "ux_resume_events_resume_version",
                        "ux_resume_events_ai_task_id",
                        "ix_resume_events_preview_lookup",
                        "ix_resume_events_pending_lookup",
                        "ix_resume_events_user_status_created");
    }

    @Test
    void save_rejectsDuplicateResumeIdAndVersionNo() {
        resumeEventMongoRepository.save(createEvent(1L, 1, 10L, ResumeVersionStatus.QUEUED, "{}"));

        assertThatThrownBy(
                        () ->
                                resumeEventMongoRepository.save(
                                        createEvent(
                                                1L, 1, 10L, ResumeVersionStatus.PROCESSING, "{}")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void save_rejectsDuplicateAiTaskId() {
        ResumeEventDocument first = createEvent(1L, 1, 10L, ResumeVersionStatus.QUEUED, "{}");
        first.startProcessing("job-1", Instant.parse("2026-03-17T00:00:00Z"));
        resumeEventMongoRepository.save(first);

        ResumeEventDocument second = createEvent(1L, 2, 10L, ResumeVersionStatus.QUEUED, "{}");
        second.startProcessing("job-1", Instant.parse("2026-03-17T00:01:00Z"));

        assertThatThrownBy(() -> resumeEventMongoRepository.save(second))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void findLatestSucceededByResumeId_returnsHighestSucceededVersion() {
        resumeEventMongoRepository.save(createEvent(1L, 1, 10L, ResumeVersionStatus.FAILED, "{}"));
        resumeEventMongoRepository.save(
                createEvent(1L, 2, 10L, ResumeVersionStatus.SUCCEEDED, "{\"v\":2}"));
        resumeEventMongoRepository.save(
                createEvent(1L, 3, 10L, ResumeVersionStatus.SUCCEEDED, "{\"v\":3}"));

        ResumeEventDocument found =
                resumeEventQueryRepository.findLatestSucceededByResumeId(1L).orElseThrow();

        assertThat(found.getVersionNo()).isEqualTo(3);
        assertThat(found.getSnapshot()).isEqualTo("{\"v\":3}");
    }

    @Test
    void findLatestUnseenPreviewByResumeId_returnsHighestUncommittedAndUnseenSucceededVersion() {
        ResumeEventDocument oldPreview =
                createEvent(1L, 2, 10L, ResumeVersionStatus.SUCCEEDED, "{\"v\":2}");
        oldPreview.markPreviewShown(Instant.parse("2026-03-17T00:10:00Z"));
        resumeEventMongoRepository.save(oldPreview);

        ResumeEventDocument committed =
                createEvent(1L, 3, 10L, ResumeVersionStatus.SUCCEEDED, "{\"v\":3}");
        committed.markCommitted(Instant.parse("2026-03-17T00:11:00Z"));
        resumeEventMongoRepository.save(committed);

        resumeEventMongoRepository.save(
                createEvent(1L, 4, 10L, ResumeVersionStatus.SUCCEEDED, "{\"v\":4}"));

        ResumeEventDocument found =
                resumeEventQueryRepository.findLatestUnseenPreviewByResumeId(1L).orElseThrow();

        assertThat(found.getVersionNo()).isEqualTo(4);
    }

    @Test
    void findProcessingByUserIds_returnsLimitedProcessingEventsOrderedByCreatedAt() {
        ResumeEventDocument first = createEvent(11L, 1, 100L, ResumeVersionStatus.QUEUED, "{}");
        first.startProcessing("job-11", Instant.parse("2026-03-17T00:00:00Z"));
        resumeEventMongoRepository.save(first);

        ResumeEventDocument second = createEvent(12L, 1, 100L, ResumeVersionStatus.QUEUED, "{}");
        second.startProcessing("job-12", Instant.parse("2026-03-17T00:01:00Z"));
        resumeEventMongoRepository.save(second);

        ResumeEventDocument third = createEvent(13L, 1, 101L, ResumeVersionStatus.QUEUED, "{}");
        third.startProcessing("job-13", Instant.parse("2026-03-17T00:02:00Z"));
        resumeEventMongoRepository.save(third);

        List<ResumeEventDocument> found =
                resumeEventQueryRepository.findProcessingByUserIds(List.of(100L, 101L), 2);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(ResumeEventDocument::getResumeId).containsExactly(11L, 12L);
    }

    private ResumeEventDocument createEvent(
            Long resumeId,
            Integer versionNo,
            Long userId,
            ResumeVersionStatus status,
            String snapshot) {
        ResumeEventDocument document =
                ResumeEventDocument.create(resumeId, versionNo, userId, status, snapshot);
        if (status == ResumeVersionStatus.SUCCEEDED) {
            document.succeed(snapshot, Instant.parse("2026-03-17T00:00:00Z"));
        } else if (status == ResumeVersionStatus.FAILED) {
            document.fail("failed", Instant.parse("2026-03-17T00:00:00Z"));
        }
        return document;
    }
}
