package com.sipomeokjo.commitme.domain.worker.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.service.ResumeAiRequestService;
import com.sipomeokjo.commitme.domain.resume.service.ResumeProjectionService;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobRequestedPayload;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiJobRequestedWorkerTest {

    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeAiRequestService resumeAiRequestService;
    @Mock private ResumeProjectionService resumeProjectionService;

    private AiJobRequestedWorker aiJobRequestedWorker;

    @BeforeEach
    void setUp() {
        aiJobRequestedWorker =
                new AiJobRequestedWorker(
                        resumeEventMongoRepository,
                        resumeAiRequestService,
                        resumeProjectionService);
    }

    @Test
    void handle_whenDispatchFails_clearsPending() {
        AiJobRequestedPayload payload =
                new AiJobRequestedPayload(100L, 1, 11L, "Backend", List.of("https://github.com/x"));
        ResumeEventDocument queued =
                ResumeEventDocument.create(100L, 1, 11L, ResumeVersionStatus.QUEUED, "{}");

        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 1))
                .willReturn(Optional.of(queued), Optional.of(queued));
        given(resumeAiRequestService.requestGenerateJob(11L, "Backend", payload.repoUrls()))
                .willReturn(
                        ResumeAiRequestService.DispatchResult.failed(
                                "AI_GENERATE_FAILED", "dispatch failed"));

        aiJobRequestedWorker.handle(payload);

        verify(resumeEventMongoRepository).save(queued);
        verify(resumeProjectionService).applyAiFailure(100L, 1);
    }
}
