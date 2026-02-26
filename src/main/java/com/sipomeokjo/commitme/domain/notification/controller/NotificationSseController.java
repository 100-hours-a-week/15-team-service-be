package com.sipomeokjo.commitme.domain.notification.controller;

import com.sipomeokjo.commitme.domain.notification.service.NotificationSseService;
import com.sipomeokjo.commitme.security.resolver.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationSseController {
    private final NotificationSseService notificationSseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @CurrentUserId Long userId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return notificationSseService.subscribe(userId, lastEventId);
    }
}
