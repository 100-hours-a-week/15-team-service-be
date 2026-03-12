package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionCacheRefreshResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PositionCacheRefreshService {
    private final PositionCacheRefreshPublisher positionCacheRefreshPublisher;
    private final PositionCacheWarmupService positionCacheWarmupService;

    public PositionCacheRefreshResponse refreshAllInstances() {
        boolean localFallbackTriggered = false;
        long notifiedSubscriberCount = 0L;

        try {
            notifiedSubscriberCount =
                    positionCacheRefreshPublisher.publishRefresh("manual_refresh");
            if (notifiedSubscriberCount == 0L) {
                localFallbackTriggered = true;
                positionCacheWarmupService.refreshAsync("manual_refresh_no_subscriber");
                log.warn(
                        "[PositionCacheRefresh] no_subscriber localFallbackTriggered=true channel={}",
                        PositionCacheChannels.REFRESH_CHANNEL);
            }
        } catch (RuntimeException ex) {
            localFallbackTriggered = true;
            positionCacheWarmupService.refreshAsync("manual_refresh_publish_failed");
            log.warn("[PositionCacheRefresh] publish_failed localFallbackTriggered=true", ex);
        }

        return new PositionCacheRefreshResponse(notifiedSubscriberCount, localFallbackTriggered);
    }
}
