package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.resume.document.ResumeDocument;
import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeMongoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeDiffServiceTest {

    @Mock private ResumeMongoRepository resumeMongoRepository;
    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;

    private ResumeDiffService resumeDiffService;

    @BeforeEach
    void setUp() {
        resumeDiffService =
                new ResumeDiffService(
                        resumeMongoRepository, resumeEventMongoRepository, new ObjectMapper());
    }

    @Test
    void getDiff_whenSnapshotIsMalformed_throwsInternalServerError() {
        ResumeDocument document = org.mockito.Mockito.mock(ResumeDocument.class);
        ResumeEventDocument baseEvent =
                ResumeEventDocument.create(100L, 1, 1L, ResumeVersionStatus.QUEUED, "{}");
        ResumeEventDocument targetEvent =
                ResumeEventDocument.create(100L, 2, 1L, ResumeVersionStatus.QUEUED, "{}");
        Instant committedAt = Instant.parse("2026-03-29T00:00:00Z");

        baseEvent.succeed("{\"summary\":\"base\"}", committedAt);
        baseEvent.markCommitted(committedAt);
        targetEvent.succeed("{", committedAt);
        targetEvent.markCommitted(committedAt);

        given(resumeMongoRepository.findByResumeId(100L)).willReturn(Optional.of(document));
        given(document.getUserId()).willReturn(1L);
        given(document.getCurrentVersionNo()).willReturn(1);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(targetEvent));
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 1))
                .willReturn(Optional.of(baseEvent));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> resumeDiffService.getDiff(1L, 100L, 2, "current"));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
