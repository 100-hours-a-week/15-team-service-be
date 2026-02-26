package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.domain.notification.dto.NotificationBadgeResponse;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationSeenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationBadgeService {
    private final NotificationRepository notificationRepository;
    private final NotificationSeenRepository notificationSeenRepository;

    public NotificationBadgeResponse getBadge(Long userId) {
        Long latestId =
                notificationRepository
                        .findTopByUser_IdOrderByIdDesc(userId)
                        .map(Notification::getId)
                        .orElse(0L);
        Long lastSeenId = notificationSeenRepository.findLastSeenIdByUserId(userId);
        if (lastSeenId == null) {
            lastSeenId = 0L;
        }
        boolean hasNew = latestId > lastSeenId;
        return new NotificationBadgeResponse(hasNew, latestId);
    }
}
