package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.service.NotificationEventPublisher;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCallbackSource;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCompletionEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeCompletionPostProcessListener {
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationEventPublisher notificationEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ResumeCompletionEvent event) {
        if (event == null
                || event.userId() == null
                || event.resumeId() == null
                || event.status() == null) {
            return;
        }

        if (event.source() == ResumeCallbackSource.EDIT) {
            bridgeEditEvent(event);
        }

        if (event.status() == ResumeVersionStatus.SUCCEEDED) {
            publishNotification(event);
        }
    }

    private void bridgeEditEvent(ResumeCompletionEvent event) {
        if (event.status() == ResumeVersionStatus.SUCCEEDED) {
            eventPublisher.publishEvent(
                    new ResumeEditCompletedEvent(
                            event.userId(),
                            event.resumeId(),
                            event.versionNo(),
                            event.taskId(),
                            event.updatedAt()));
            return;
        }
        if (event.status() == ResumeVersionStatus.FAILED) {
            eventPublisher.publishEvent(
                    new ResumeEditFailedEvent(
                            event.userId(),
                            event.resumeId(),
                            event.versionNo(),
                            event.taskId(),
                            event.updatedAt(),
                            event.errorCode(),
                            event.errorMessage()));
        }
    }

    private void publishNotification(ResumeCompletionEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "RESUME_COMPLETED");
        payload.put("source", event.source().name());
        payload.put("resumeId", event.resumeId());
        payload.put("versionNo", event.versionNo());
        payload.put("taskId", event.taskId());
        payload.put("completedAt", event.updatedAt());
        notificationEventPublisher.publish(event.userId(), NotificationType.RESUME, payload);
        log.debug(
                "[RESUME_POST_PROCESS] notification_published userId={} resumeId={} versionNo={} source={}",
                event.userId(),
                event.resumeId(),
                event.versionNo(),
                event.source());
    }
}
