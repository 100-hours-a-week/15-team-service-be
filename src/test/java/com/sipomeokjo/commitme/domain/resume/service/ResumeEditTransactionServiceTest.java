package com.sipomeokjo.commitme.domain.resume.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.domain.resume.document.ResumeEventDocument;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.repository.mongo.ResumeEventMongoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeEditTransactionServiceTest {

    @Mock private ResumeEventMongoRepository resumeEventMongoRepository;
    @Mock private ResumeProjectionService resumeProjectionService;

    private ResumeEditTransactionService resumeEditTransactionService;

    @BeforeEach
    void setUp() {
        resumeEditTransactionService =
                new ResumeEditTransactionService(
                        resumeEventMongoRepository, resumeProjectionService);
    }

    @Test
    void markEditFailed_clearsProjectionPending() {
        ResumeEventDocument event =
                ResumeEventDocument.create(100L, 2, 11L, ResumeVersionStatus.PROCESSING, "{}");

        given(resumeEventMongoRepository.findByResumeIdAndVersionNo(100L, 2))
                .willReturn(Optional.of(event));

        resumeEditTransactionService.markEditFailed(100L, 2, "boom");

        verify(resumeEventMongoRepository).save(event);
        verify(resumeProjectionService).applyAiFailure(100L, 2);
    }
}
