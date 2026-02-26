package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public void markRead(Long userId, Long notificationId) {
        if (notificationId == null || notificationId < 1) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        int updated = notificationRepository.markRead(notificationId, userId, Instant.now(clock));
        if (updated > 0) {
            return;
        }

        boolean exists =
                notificationRepository.findByIdAndUser_Id(notificationId, userId).isPresent();
        if (!exists) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
