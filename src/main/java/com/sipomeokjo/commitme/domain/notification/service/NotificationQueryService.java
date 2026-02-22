package com.sipomeokjo.commitme.domain.notification.service;

import com.sipomeokjo.commitme.api.pagination.CursorParser;
import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationBadgeResponse;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationListResponse;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.mapper.NotificationMapper;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationQueryService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CursorParser cursorParser;
    private final NotificationBadgeService notificationBadgeService;

    public NotificationListResponse list(Long userId, CursorRequest request) {
        int size = CursorRequest.resolveLimit(request, 20);
        Long cursorId = cursorParser.parseIdCursor(request == null ? null : request.next());

        List<Notification> notifications =
                (cursorId == null)
                        ? notificationRepository.findByUser_IdOrderByIdDesc(
                                userId, PageRequest.of(0, size + 1))
                        : notificationRepository.findByUser_IdAndIdLessThanOrderByIdDesc(
                                userId, cursorId, PageRequest.of(0, size + 1));

        boolean hasNext = notifications.size() > size;
        List<Notification> pageItems = hasNext ? notifications.subList(0, size) : notifications;

        Long nextCursor = hasNext ? pageItems.getLast().getId() : null;
        Long latestId =
                notificationRepository
                        .findTopByUser_IdOrderByIdDesc(userId)
                        .map(Notification::getId)
                        .orElse(0L);

        return new NotificationListResponse(
                latestId,
                pageItems.stream().map(notificationMapper::toItem).toList(),
                nextCursor,
                hasNext);
    }

    public NotificationBadgeResponse badge(Long userId) {
        return notificationBadgeService.getBadge(userId);
    }
}
