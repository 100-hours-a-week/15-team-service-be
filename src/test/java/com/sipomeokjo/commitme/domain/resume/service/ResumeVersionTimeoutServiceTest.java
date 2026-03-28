package com.sipomeokjo.commitme.domain.resume.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeVersionTimeoutServiceTest {

    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeProjectionService resumeProjectionService;
    @Mock private ResumeLockService resumeLockService;

    private ResumeVersionTimeoutService resumeVersionTimeoutService;

    @BeforeEach
    void setUp() {
        resumeVersionTimeoutService =
                new ResumeVersionTimeoutService(
                        resumeEventMongoRepository, resumeProjectionService, resumeLockService);
    }

    @Test
    void sweepTimeoutVersions_whenCreateEventTimesOut_clearsPendingAndReleasesCreateLock() {
        ResumeEventDocument event = org.mockito.Mockito.mock(ResumeEventDocument.class);
        given(
                        resumeEventMongoRepository.findByStatusIn(
                                List.of(
                                        ResumeVersionStatus.QUEUED,
                                        ResumeVersionStatus.PROCESSING)))
                .willReturn(List.of(event));
        given(event.isProcessingTimedOut(ResumeVersionTimeoutService.AI_PROCESSING_TIMEOUT_MINUTES))
                .willReturn(false);
        given(event.isQueuedTimedOut(ResumeVersionTimeoutService.AI_PROCESSING_TIMEOUT_MINUTES))
                .willReturn(true);
        given(event.getResumeId()).willReturn(100L);
        given(event.getVersionNo()).willReturn(1);
        given(event.getUserId()).willReturn(11L);

        resumeVersionTimeoutService.sweepTimeoutVersions();

        verify(resumeEventMongoRepository).save(event);
        verify(resumeProjectionService).applyAiFailure(100L, 1);
        verify(resumeLockService).releaseCreateLock(11L, 100L);
    }

    @Test
    void sweepTimeoutVersions_whenEditEventTimesOut_clearsPendingAndReleasesEditLock() {
        ResumeEventDocument event = org.mockito.Mockito.mock(ResumeEventDocument.class);
        given(
                        resumeEventMongoRepository.findByStatusIn(
                                List.of(
                                        ResumeVersionStatus.QUEUED,
                                        ResumeVersionStatus.PROCESSING)))
                .willReturn(List.of(event));
        given(event.isProcessingTimedOut(ResumeVersionTimeoutService.AI_PROCESSING_TIMEOUT_MINUTES))
                .willReturn(true);
        given(event.getResumeId()).willReturn(100L);
        given(event.getVersionNo()).willReturn(3);

        resumeVersionTimeoutService.sweepTimeoutVersions();

        verify(resumeEventMongoRepository).save(event);
        verify(resumeProjectionService).applyAiFailure(100L, 3);
        verify(resumeLockService).releaseEditLock(100L, 3);
    }
}
