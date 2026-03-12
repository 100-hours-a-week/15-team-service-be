package com.sipomeokjo.commitme.domain.position.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sipomeokjo.commitme.domain.position.dto.PositionCacheRefreshResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionCacheAdminServiceTest {

    @Mock private PositionCacheRefreshPublisher positionCacheRefreshPublisher;
    @Mock private PositionCacheWarmupService positionCacheWarmupService;

    private PositionCacheAdminService positionCacheAdminService;

    @BeforeEach
    void setUp() {
        positionCacheAdminService =
                new PositionCacheAdminService(
                        positionCacheRefreshPublisher, positionCacheWarmupService);
    }

    @Test
    void refreshAllInstances_publishSuccessWithoutFallback_returnsSubscriberCount() {
        given(positionCacheRefreshPublisher.publishRefresh("manual_refresh")).willReturn(2L);

        PositionCacheRefreshResponse response = positionCacheAdminService.refreshAllInstances();

        assertThat(response.notifiedSubscriberCount()).isEqualTo(2L);
        assertThat(response.localFallbackTriggered()).isFalse();
        verifyNoInteractions(positionCacheWarmupService);
    }

    @Test
    void refreshAllInstances_publishFailure_triggersLocalFallback() {
        given(positionCacheRefreshPublisher.publishRefresh("manual_refresh"))
                .willThrow(new IllegalStateException("redis down"));

        PositionCacheRefreshResponse response = positionCacheAdminService.refreshAllInstances();

        assertThat(response.notifiedSubscriberCount()).isZero();
        assertThat(response.localFallbackTriggered()).isTrue();
        verify(positionCacheWarmupService).refreshAsync("manual_refresh_publish_failed");
    }
}
