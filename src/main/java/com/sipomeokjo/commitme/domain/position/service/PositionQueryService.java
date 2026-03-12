package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.mapper.PositionMapper;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PositionQueryService {
    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final PositionLocalCache positionLocalCache;
    private final MeterRegistry meterRegistry;
    private final Object cacheLoadMonitor = new Object();

    public List<PositionResponse> getPositions() {
        List<PositionResponse> cachedPositions = positionLocalCache.getAll();
        if (cachedPositions != null) {
            meterRegistry.counter("positions_cache_requests_total", "result", "hit").increment();
            return cachedPositions;
        }

        meterRegistry.counter("positions_cache_requests_total", "result", "miss").increment();
        return loadAndCachePositions();
    }

    public int warmUpCache() {
        return getPositions().size();
    }

    public boolean hasCachedPositions() {
        return positionLocalCache.containsAll();
    }

    public boolean evictAllCachedPositions() {
        return positionLocalCache.evictAll();
    }

    private List<PositionResponse> loadAndCachePositions() {
        synchronized (cacheLoadMonitor) {
            List<PositionResponse> cachedPositions = positionLocalCache.getAll();
            if (cachedPositions != null) {
                return cachedPositions;
            }

            long startedAtNanos = System.nanoTime();
            try {
                List<PositionResponse> positions =
                        positionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                                .map(positionMapper::toResponse)
                                .toList();
                positionLocalCache.putAll(positions);
                meterRegistry
                        .counter("positions_cache_write_total", "result", "success")
                        .increment();
                recordLoadDuration(startedAtNanos, "success");
                return positions;
            } catch (RuntimeException ex) {
                meterRegistry
                        .counter("positions_cache_write_total", "result", "failed")
                        .increment();
                recordLoadDuration(startedAtNanos, "failed");
                throw ex;
            }
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
