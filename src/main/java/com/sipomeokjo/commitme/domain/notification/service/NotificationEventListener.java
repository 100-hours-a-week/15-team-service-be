package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.event.NotificationCreateEvent;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreate(NotificationCreateEvent event) {
        if (event == null || event.userId() == null || event.type() == null) {
            return;
        }

        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) {
            log.warn("[NOTIFICATION] user_not_found userId={}", event.userId());
            return;
        }

        notificationRepository.save(Notification.create(user, event.type(), event.payload()));
    }
}
