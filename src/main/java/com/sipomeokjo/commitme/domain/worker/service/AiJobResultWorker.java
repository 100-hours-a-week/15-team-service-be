package com.sipomeokjo.commitme.domain.worker.service;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.entity.NotificationType;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.notification.service.NotificationSseService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.domain.worker.dto.AiJobResultPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobResultWorker {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseService notificationSseService;

    @Transactional
    public WorkerHandleResult handle(AiJobResultPayload payload) {
        if (payload == null
                || payload.userId() == null
                || payload.notificationPayloadJson() == null) {
            return WorkerHandleResult.invalid("payload_missing_required_field");
        }

        User user = userRepository.findById(payload.userId()).orElse(null);
        if (user == null) {
            return WorkerHandleResult.skipped("notification_user_not_found");
        }

        Notification saved =
                notificationRepository.save(
                        Notification.create(
                                user, NotificationType.RESUME, payload.notificationPayloadJson()));
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            notificationSseService.send(saved);
                        } catch (Exception ex) {
                            log.warn(
                                    "[AI_JOB_RESULT_WORKER] notification_sse_failed notificationId={} userId={}",
                                    saved.getId(),
                                    payload.userId(),
                                    ex);
                        }
                    }
                });

        return WorkerHandleResult.success();
    }
}
