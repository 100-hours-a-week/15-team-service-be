package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.notification.service.NotificationSseService;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeRefreshRequiredSsePayload;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeSseService {
    private static final String STATUS_SUCCEEDED = "SUCCEEDED";
    private static final String STATUS_FAILED = "FAILED";

    private final NotificationSseService notificationSseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEditCompleted(ResumeEditCompletedEvent event) {
        if (event == null || event.userId() == null || event.resumeId() == null) {
            return;
        }
        log.debug(
                "[RESUME_SSE] bridge_edit_completed userId={} resumeId={} versionNo={} taskId={}",
                event.userId(),
                event.resumeId(),
                event.versionNo(),
                event.taskId());
        notificationSseService.sendResumeRefreshRequired(
                event.userId(),
                new ResumeRefreshRequiredSsePayload(
                        event.resumeId(), event.versionNo(), STATUS_SUCCEEDED));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEditFailed(ResumeEditFailedEvent event) {
        if (event == null || event.userId() == null || event.resumeId() == null) {
            return;
        }
        log.debug(
                "[RESUME_SSE] bridge_edit_failed userId={} resumeId={} versionNo={} taskId={} errorCode={}",
                event.userId(),
                event.resumeId(),
                event.versionNo(),
                event.taskId(),
                event.errorCode());
        notificationSseService.sendResumeRefreshRequired(
                event.userId(),
                new ResumeRefreshRequiredSsePayload(
                        event.resumeId(), event.versionNo(), STATUS_FAILED));
    }
}
