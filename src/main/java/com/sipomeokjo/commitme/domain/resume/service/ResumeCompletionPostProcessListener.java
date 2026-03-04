package com.sipomeokjo.commitme.domain.resume.service;

import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.service.NotificationEventPublisher;
import com.sipomeokjo.commitme.domain.resume.entity.Resume;
import com.sipomeokjo.commitme.domain.resume.entity.ResumeVersionStatus;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCallbackSource;
import com.sipomeokjo.commitme.domain.resume.event.ResumeCompletionEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditCompletedEvent;
import com.sipomeokjo.commitme.domain.resume.event.ResumeEditFailedEvent;
import com.sipomeokjo.commitme.domain.resume.repository.ResumeRepository;
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
    private final ResumeRepository resumeRepository;

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
        String resumeName =
                resumeRepository
                        .findByIdAndUser_Id(event.resumeId(), event.userId())
                        .map(Resume::getName)
                        .orElse("이력서");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "RESUME_COMPLETED");
        payload.put("source", event.source().name());
        payload.put("resumeId", event.resumeId());
        payload.put("resumeName", resumeName);
        payload.put("versionNo", event.versionNo());
        payload.put("status", event.status().name());
        payload.put("message", buildCompletionMessage(event.source(), resumeName));
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

    private String buildCompletionMessage(ResumeCallbackSource source, String resumeName) {
        if (source == ResumeCallbackSource.EDIT) {
            return resumeName + " 이력서의 수정이 완료되었습니다.";
        }
        return resumeName + " 이력서의 생성이 완료되었습니다.";
    }
}
