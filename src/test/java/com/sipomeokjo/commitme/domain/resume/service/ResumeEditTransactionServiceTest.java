package com.sipomeokjo.commitme.domain.resume.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventQueryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeEditTransactionServiceTest {

    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeEventQueryRepository resumeEventQueryRepository;
    @Mock private ResumeProjectionService resumeProjectionService;

    private ResumeEditTransactionService resumeEditTransactionService;

    @BeforeEach
    void setUp() {
        resumeEditTransactionService =
                new ResumeEditTransactionService(
                        resumeEventMongoRepository,
                        resumeEventQueryRepository,
                        resumeProjectionService);
    }

    @Test
    void markEditFailed_clearsPendingProjectionAlongsideEventFailure() {
        ResumeEventDocument event = org.mockito.Mockito.mock(ResumeEventDocument.class);
        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(event));

        resumeEditTransactionService.markEditFailed(100L, 2, "ai timeout");

        verify(event).failNow("AI_EDIT_FAILED", "ai timeout");
        verify(resumeEventMongoRepository).save(event);
        verify(resumeProjectionService).applyAiFailure(100L, 2);
    }

    @Test
    void markEditRequested_bindsAiTaskIdViaAtomicQuery() {
        ResumeEventDocument updated = org.mockito.Mockito.mock(ResumeEventDocument.class);
        given(
                        resumeEventQueryRepository.bindAiTaskIdAndStartProcessing(
                                eq(100L), eq(2), eq("task-1"), any()))
                .willReturn(Optional.of(updated));

        resumeEditTransactionService.markEditRequested(100L, 2, "task-1");

        verify(resumeEventQueryRepository)
                .bindAiTaskIdAndStartProcessing(eq(100L), eq(2), eq("task-1"), any());
    }

    @Test
    void markEditRequested_whenBindingFails_throwsIllegalStateException() {
        given(
                        resumeEventQueryRepository.bindAiTaskIdAndStartProcessing(
                                eq(100L), eq(2), eq("task-1"), any()))
                .willReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> resumeEditTransactionService.markEditRequested(100L, 2, "task-1"));
    }
}
