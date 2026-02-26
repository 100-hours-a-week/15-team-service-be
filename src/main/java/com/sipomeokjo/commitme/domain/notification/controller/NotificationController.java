package com.sipomeokjo.commitme.domain.notification.controller;

import com.sipomeokjo.commitme.api.pagination.CursorRequest;
import com.sipomeokjo.commitme.api.response.APIResponse;
import com.sipomeokjo.commitme.api.response.SuccessCode;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationBadgeResponse;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationListResponse;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationSeenRequest;
import com.sipomeokjo.commitme.domain.notification.service.NotificationCommandService;
import com.sipomeokjo.commitme.domain.notification.service.NotificationQueryService;
import com.sipomeokjo.commitme.domain.notification.service.NotificationSeenCommandService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationQueryService notificationQueryService;
    private final NotificationSeenCommandService notificationSeenCommandService;
    private final NotificationCommandService notificationCommandService;

    @GetMapping
    public ResponseEntity<APIResponse<NotificationListResponse>> list(
            @CurrentUserId Long userId, CursorRequest request) {
        return APIResponse.onSuccess(
                SuccessCode.OK, notificationQueryService.list(userId, request));
    }

    @GetMapping("/badge")
    public ResponseEntity<APIResponse<NotificationBadgeResponse>> badge(
            @CurrentUserId Long userId) {
        return APIResponse.onSuccess(SuccessCode.OK, notificationQueryService.badge(userId));
    }

    @PatchMapping("/seen")
    public ResponseEntity<APIResponse<Void>> markSeen(
            @CurrentUserId Long userId, @RequestBody NotificationSeenRequest request) {
        notificationSeenCommandService.markSeenUpTo(userId, request.upToId());
        return APIResponse.onSuccess(SuccessCode.OK);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<APIResponse<Void>> markRead(
            @CurrentUserId Long userId, @PathVariable Long id) {
        notificationCommandService.markRead(userId, id);
        return APIResponse.onSuccess(SuccessCode.OK);
    }
}
