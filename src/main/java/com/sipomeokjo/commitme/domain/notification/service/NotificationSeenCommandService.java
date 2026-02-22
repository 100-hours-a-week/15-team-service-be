package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationSeenRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSeenCommandService {
    private final NotificationSeenRepository notificationSeenRepository;
    private final Clock clock;

    public void markSeenUpTo(Long userId, Long upToId) {
        if (upToId == null || upToId < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Instant now = Instant.now(clock);
        notificationSeenRepository.upsertSeen(userId, upToId, now);
    }
}
