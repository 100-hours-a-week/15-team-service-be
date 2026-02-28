package com.sipomeokjo.commitme.domain.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sipomeokjo.commitme.api.sse.SseEmitterRegistry;
import com.sipomeokjo.commitme.api.sse.SseExceptionUtils;
import com.sipomeokjo.commitme.api.sse.SseStreamKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryBus;
import com.sipomeokjo.commitme.api.sse.distributed.SseDeliveryEnvelope;
import com.sipomeokjo.commitme.api.sse.distributed.SseInstanceIdProvider;
import com.sipomeokjo.commitme.api.sse.distributed.SseLocalDeliveryHandler;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteKey;
import com.sipomeokjo.commitme.api.sse.distributed.SseRouteRepository;
import com.sipomeokjo.commitme.api.sse.dto.UserSseEventName;
import com.sipomeokjo.commitme.api.sse.dto.UserSseEventPayload;
import com.sipomeokjo.commitme.api.sse.dto.UserSseEventType;
import com.sipomeokjo.commitme.domain.notification.dto.NotificationSsePayload;
import com.sipomeokjo.commitme.domain.notification.entity.Notification;
import com.sipomeokjo.commitme.domain.notification.repository.NotificationRepository;
import com.sipomeokjo.commitme.domain.resume.dto.ResumeRefreshRequiredSsePayload;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseService implements SseLocalDeliveryHandler {
    private static final String STREAM_TYPE_USER_EVENT = "user-event";
    private static final Duration ROUTE_TTL = Duration.ofMinutes(2);

    private final NotificationRepository notificationRepository;
    private final NotificationBadgeService notificationBadgeService;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final SseRouteRepository sseRouteRepository;
    private final SseDeliveryBus sseDeliveryBus;
    private final SseInstanceIdProvider sseInstanceIdProvider;

    public SseEmitter subscribe(Long userId, String lastEventId) {
        log.debug(
                "[NOTIFICATION_SSE] subscribe_start userId={} hasLastEventId={}",
                userId,
                lastEventId != null && !lastEventId.isBlank());
        SseStreamKey streamKey = streamKey(userId);
        SseEmitter emitter = sseEmitterRegistry.register(streamKey);
        refreshRouteTtl(streamKey);

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(UserSseEventName.CONNECTED.value())
                            .data(
                                    new UserSseEventPayload(
                                            UserSseEventType.CONNECTED,
                                            Instant.now(),
                                            objectMapper.valueToTree(
                                                    notificationBadgeService.getBadge(userId)))));
        } catch (Exception ex) {
            if (SseExceptionUtils.isClientDisconnected(ex)) {
                log.debug(
                        "[NOTIFICATION_SSE] connected_event_client_disconnected userId={}", userId);
            } else {
                log.warn("[NOTIFICATION_SSE] connected_event_failed userId={}", userId, ex);
            }
            sseEmitterRegistry.completeWithError(streamKey, emitter, ex);
            return emitter;
        }

        Long lastId = parseLastEventId(lastEventId);
        if (lastId != null) {
            log.debug(
                    "[NOTIFICATION_SSE] replay_requested userId={} lastEventId={}", userId, lastId);
            replay(userId, lastId, emitter);
        }

        return emitter;
    }

    public void send(Notification notification) {
        if (notification == null || notification.getUser() == null) {
            return;
        }
        Long userId = notification.getUser().getId();
        if (userId == null) {
            return;
        }

        NotificationSsePayload payload = toPayload(notification);
        log.debug(
                "[NOTIFICATION_SSE] send_notification userId={} notificationId={} type={}",
                userId,
                notification.getId(),
                notification.getType());
        sendDistributed(
                userId,
                UserSseEventName.NOTIFICATION.value(),
                String.valueOf(notification.getId()),
                new UserSseEventPayload(
                        UserSseEventType.NOTIFICATION_CREATED,
                        notification.getCreatedAt(),
                        objectMapper.valueToTree(payload)),
                notification.getId());
    }

    public void sendResumeRefreshRequired(Long userId, ResumeRefreshRequiredSsePayload payload) {
        if (userId == null || payload == null) {
            return;
        }
        log.debug(
                "[NOTIFICATION_SSE] send_resume_refresh_required userId={} resumeId={} versionNo={} status={}",
                userId,
                payload.resumeId(),
                payload.versionNo(),
                payload.status());
        sendDistributed(
                userId,
                UserSseEventName.RESUME_REFRESH_REQUIRED.value(),
                null,
                new UserSseEventPayload(
                        UserSseEventType.RESUME_REFRESH_REQUIRED,
                        Instant.now(),
                        objectMapper.valueToTree(payload)),
                null);
    }

    @Override
    public String streamType() {
        return STREAM_TYPE_USER_EVENT;
    }

    @Override
    public void deliver(SseDeliveryEnvelope envelope) {
        if (envelope == null) {
            return;
        }
        log.debug(
                "[NOTIFICATION_SSE] remote_delivery_received streamKey={} eventName={} eventId={}",
                envelope.streamKey(),
                envelope.eventName(),
                envelope.eventId());
        Long userId = parseUserId(envelope.streamKey());
        if (userId == null) {
            return;
        }

        UserSseEventPayload payload;
        try {
            if (envelope.data() == null) {
                return;
            }
            payload = objectMapper.treeToValue(envelope.data(), UserSseEventPayload.class);
        } catch (Exception ex) {
            log.warn(
                    "[NOTIFICATION_SSE] remote_payload_parse_failed streamKey={} eventName={}",
                    envelope.streamKey(),
                    envelope.eventName(),
                    ex);
            return;
        }

        sendLocalOnly(userId, envelope.eventName(), envelope.eventId(), payload, null);
    }

    private void sendDistributed(
            Long userId,
            String eventName,
            String eventId,
            UserSseEventPayload payload,
            Long notificationId) {
        SseStreamKey localStreamKey = streamKey(userId);
        SseRouteKey routeKey = routeKey(userId);
        String localInstanceId = sseInstanceIdProvider.getInstanceId();

        java.util.Set<String> instanceIds;
        try {
            instanceIds = sseRouteRepository.findInstanceIds(routeKey);
        } catch (Exception ex) {
            log.warn("[NOTIFICATION_SSE] route_lookup_failed userId={}", userId, ex);
            sendLocalOnly(userId, eventName, eventId, payload, notificationId);
            return;
        }

        if (instanceIds.isEmpty()) {
            log.debug(
                    "[NOTIFICATION_SSE] route_instances_empty userId={} eventName={}",
                    userId,
                    eventName);
            sendLocalOnly(userId, eventName, eventId, payload, notificationId);
            return;
        }

        JsonNode payloadNode = objectMapper.valueToTree(payload);
        boolean localDelivered = false;
        for (String instanceId : instanceIds) {
            if (localInstanceId.equals(instanceId)) {
                sendLocalOnly(userId, eventName, eventId, payload, notificationId);
                localDelivered = true;
                continue;
            }

            try {
                sseDeliveryBus.publish(
                        new SseDeliveryEnvelope(
                                localInstanceId,
                                instanceId,
                                STREAM_TYPE_USER_EVENT,
                                String.valueOf(userId),
                                eventName,
                                eventId,
                                payloadNode,
                                null));
            } catch (Exception ex) {
                log.warn(
                        "[NOTIFICATION_SSE] remote_publish_failed userId={} notificationId={} targetInstanceId={}",
                        userId,
                        notificationId,
                        instanceId,
                        ex);
            }
        }

        if (!localDelivered && sseEmitterRegistry.count(localStreamKey) > 0) {
            sendLocalOnly(userId, eventName, eventId, payload, notificationId);
        }
    }

    private void sendLocalOnly(
            Long userId,
            String eventName,
            String eventId,
            UserSseEventPayload payload,
            Long notificationId) {
        SseStreamKey streamKey = streamKey(userId);
        List<SseEmitter> emitters = sseEmitterRegistry.getEmitters(streamKey);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        log.debug(
                "[NOTIFICATION_SSE] local_delivery_start userId={} eventName={} notificationId={} emitterCount={}",
                userId,
                eventName,
                notificationId,
                emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder eventBuilder =
                        SseEmitter.event().name(eventName).data(payload);
                if (eventId != null && !eventId.isBlank()) {
                    eventBuilder.id(eventId);
                }
                emitter.send(eventBuilder);
            } catch (Exception ex) {
                if (SseExceptionUtils.isClientDisconnected(ex)) {
                    log.debug(
                            "[NOTIFICATION_SSE] client_disconnected userId={} notificationId={}",
                            userId,
                            notificationId);
                } else {
                    log.warn(
                            "[NOTIFICATION_SSE] send_failed userId={} notificationId={}",
                            userId,
                            notificationId,
                            ex);
                }
                sseEmitterRegistry.completeWithError(streamKey, emitter, ex);
            }
        }
    }

    private void replay(Long userId, Long lastEventId, SseEmitter emitter) {
        SseStreamKey streamKey = streamKey(userId);
        List<Notification> notifications =
                notificationRepository.findByUser_IdAndIdGreaterThanOrderByIdAsc(
                        userId, lastEventId);
        if (notifications.isEmpty()) {
            log.debug(
                    "[NOTIFICATION_SSE] replay_empty userId={} lastEventId={}",
                    userId,
                    lastEventId);
            return;
        }
        log.debug(
                "[NOTIFICATION_SSE] replay_start userId={} lastEventId={} replayCount={}",
                userId,
                lastEventId,
                notifications.size());
        for (Notification notification : notifications) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .id(String.valueOf(notification.getId()))
                                .name(UserSseEventName.NOTIFICATION.value())
                                .data(
                                        new UserSseEventPayload(
                                                UserSseEventType.NOTIFICATION_CREATED,
                                                notification.getCreatedAt(),
                                                objectMapper.valueToTree(
                                                        toPayload(notification)))));
            } catch (Exception ex) {
                if (SseExceptionUtils.isClientDisconnected(ex)) {
                    log.debug(
                            "[NOTIFICATION_SSE] replay_client_disconnected userId={} notificationId={}",
                            userId,
                            notification.getId());
                } else {
                    log.warn(
                            "[NOTIFICATION_SSE] replay_failed userId={} notificationId={}",
                            userId,
                            notification.getId(),
                            ex);
                }
                sseEmitterRegistry.completeWithError(streamKey, emitter, ex);
                return;
            }
        }
    }

    private SseStreamKey streamKey(Long userId) {
        return SseStreamKey.of(STREAM_TYPE_USER_EVENT, userId);
    }

    private SseRouteKey routeKey(Long userId) {
        return SseRouteKey.of(STREAM_TYPE_USER_EVENT, userId);
    }

    private void refreshRouteTtl(SseStreamKey streamKey) {
        try {
            sseRouteRepository.upsertRoute(
                    SseRouteKey.of(streamKey.streamType(), streamKey.streamKey()),
                    sseInstanceIdProvider.getInstanceId(),
                    ROUTE_TTL);
        } catch (Exception ex) {
            log.debug(
                    "[NOTIFICATION_SSE] route_ttl_refresh_failed streamType={} streamKey={}",
                    streamKey.streamType(),
                    streamKey.streamKey(),
                    ex);
        }
    }

    private Long parseUserId(String streamKey) {
        try {
            return Long.parseLong(streamKey);
        } catch (Exception ex) {
            log.warn("[NOTIFICATION_SSE] stream_key_invalid streamKey={}", streamKey, ex);
            return null;
        }
    }

    private NotificationSsePayload toPayload(Notification notification) {
        JsonNode payloadNode = null;
        try {
            payloadNode = objectMapper.readTree(notification.getPayload());
        } catch (Exception ex) {
            log.warn(
                    "[NOTIFICATION_SSE] payload_parse_failed notificationId={}",
                    notification.getId(),
                    ex);
        }
        return new NotificationSsePayload(
                notification.getId(),
                notification.getType(),
                payloadNode,
                notification.getCreatedAt());
    }

    private Long parseLastEventId(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException ex) {
            log.warn("[NOTIFICATION_SSE] last_event_id_invalid value={}", lastEventId);
            return null;
        }
    }
}
