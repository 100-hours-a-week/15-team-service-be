package com.sipomeokjo.commitme.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ResumeLockServiceTest {

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private SimpleMeterRegistry meterRegistry;
    private ResumeLockService resumeLockService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        resumeLockService = new ResumeLockService(stringRedisTemplate, meterRegistry);
        Mockito.lenient().doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
    }

    @Test
    void tryAcquireCreateLock_whenScriptSucceeds_returnsAcquired() {
        doReturn(1L)
                .when(stringRedisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any(), any(), any(), any());

        ResumeLockService.LockAcquireResult result =
                resumeLockService.tryAcquireCreateLock(7L, 99L);

        assertThat(result).isEqualTo(ResumeLockService.LockAcquireResult.ACQUIRED);
    }

    @Test
    void tryAcquireEditLock_whenSetIfAbsentReportsBusy_returnsBusy() {
        doReturn(false)
                .when(valueOperations)
                .setIfAbsent("resume:lock:edit:42", "42:3", Duration.ofMinutes(5));

        ResumeLockService.LockAcquireResult result = resumeLockService.tryAcquireEditLock(42L, 3);

        assertThat(result).isEqualTo(ResumeLockService.LockAcquireResult.BUSY);
    }

    @Test
    void tryAcquireEditLock_whenRedisFails_returnsFallbackAndRecordsMetric() {
        doThrow(new IllegalStateException("redis down"))
                .when(valueOperations)
                .setIfAbsent("resume:lock:edit:42", "42:3", Duration.ofMinutes(5));

        ResumeLockService.LockAcquireResult result = resumeLockService.tryAcquireEditLock(42L, 3);

        assertThat(result).isEqualTo(ResumeLockService.LockAcquireResult.FALLBACK);
        assertThat(
                        meterRegistry
                                .get("resume.lock.fallback.total")
                                .tag("kind", "edit")
                                .counter()
                                .count())
                .isEqualTo(1.0);
    }

    @Test
    void releaseEditLock_whenMetaMissing_recordsMetric() {
        doReturn(0L)
                .when(stringRedisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), any());

        resumeLockService.releaseEditLock(42L, 3);

        assertThat(meterRegistry.get("resume.lock.release.meta_missing.total").counter().count())
                .isEqualTo(1.0);
    }
}
