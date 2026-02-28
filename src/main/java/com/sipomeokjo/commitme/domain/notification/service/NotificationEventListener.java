package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.event.NotificationCreateEvent;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationSseService notificationSseService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void handleCreate(NotificationCreateEvent event) {
        if (event == null || event.userId() == null || event.type() == null) {
            log.debug(
                    "[NOTIFICATION] create_event_skipped_invalid_input hasEvent={} hasUserId={} hasType={}",
                    event != null,
                    event != null && event.userId() != null,
                    event != null && event.type() != null);
            return;
        }
        log.debug(
                "[NOTIFICATION] create_event_received userId={} type={}",
                event.userId(),
                event.type());

        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            log.warn("[NOTIFICATION] user_not_found userId={}", event.userId());
            return;
        }

        Notification saved =
                notificationRepository.save(
                        Notification.create(user, event.type(), event.payload()));
        log.debug(
                "[NOTIFICATION] created notificationId={} userId={} type={}",
                saved.getId(),
                event.userId(),
                event.type());
        notificationSseService.send(saved);
    }
}
