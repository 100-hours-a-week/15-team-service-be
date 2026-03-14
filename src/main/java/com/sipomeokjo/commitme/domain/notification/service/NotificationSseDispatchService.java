package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseDispatchService {
    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;

    @Async("notificationSseExecutor")
    @Transactional(readOnly = true)
    public void dispatchAsync(Long notificationId) {
        if (notificationId == null) {
            return;
        }

        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.debug(
                    "[NOTIFICATION_SSE_DISPATCH] notification_not_found notificationId={}",
                    notificationId);
            return;
        }

        try {
            notificationSseService.send(notification);
        } catch (Exception ex) {
            Long userId = notification.getUser() == null ? null : notification.getUser().getId();
            log.warn(
                    "[NOTIFICATION_SSE_DISPATCH] notification_sse_failed notificationId={} userId={}",
                    notificationId,
                    userId,
                    ex);
        }
    }
}
