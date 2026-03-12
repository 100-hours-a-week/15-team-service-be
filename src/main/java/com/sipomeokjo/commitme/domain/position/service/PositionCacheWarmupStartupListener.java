package com.sipomeokjo.commitme.domain.position.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionCacheWarmupStartupListener {
    private final PositionCacheWarmupService positionCacheWarmupService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpAfterApplicationReady() {
        positionCacheWarmupService.warmUpAsync("application_ready");
    }
}
