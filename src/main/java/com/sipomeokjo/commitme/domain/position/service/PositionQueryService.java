package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.mapper.PositionMapper;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PositionQueryService {
    private static final String CACHE_NAME = "positions";
    private static final String CACHE_KEY = "all";

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    @SuppressWarnings("unchecked")
    public List<PositionResponse> getPositions() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(CACHE_KEY);
            if (valueWrapper != null) {
                Object cachedValue = valueWrapper.get();
                if (cachedValue instanceof List<?>) {
                    meterRegistry
                            .counter("positions_cache_requests_total", "result", "hit")
                            .increment();
                    return (List<PositionResponse>) cachedValue;
                }
                meterRegistry
                        .counter("positions_cache_requests_total", "result", "type_mismatch")
                        .increment();
            } else {
                meterRegistry
                        .counter("positions_cache_requests_total", "result", "miss")
                        .increment();
            }
        } else {
            meterRegistry
                    .counter("positions_cache_requests_total", "result", "no_cache")
                    .increment();
        }

        long startedAtNanos = System.nanoTime();
        try {
            List<PositionResponse> positions =
                    positionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                            .map(positionMapper::toResponse)
                            .toList();
            if (cache != null) {
                cache.put(CACHE_KEY, positions);
                meterRegistry
                        .counter("positions_cache_write_total", "result", "success")
                        .increment();
            }
            recordLoadDuration(startedAtNanos, "success");
            return positions;
        } catch (RuntimeException ex) {
            meterRegistry.counter("positions_cache_write_total", "result", "failed").increment();
            recordLoadDuration(startedAtNanos, "failed");
            throw ex;
        }
    }

    private void recordLoadDuration(long startedAtNanos, String result) {
        Timer.builder("positions_cache_load_duration")
                .tag("result", result)
                .register(meterRegistry)
                .record(
                        System.nanoTime() - startedAtNanos,
                        java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
