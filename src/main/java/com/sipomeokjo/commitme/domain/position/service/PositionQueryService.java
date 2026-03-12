package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.mapper.PositionMapper;
import com.sipomeokjo.commitme.domain.position.repository.PositionCacheRepository;
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
    private final PositionCacheRepository positionCacheRepository;
    private final MeterRegistry meterRegistry;
    private final Object cacheLoadMonitor = new Object();

    public List<PositionResponse> getPositions() {
        return positionCacheRepository
                .findAll()
                .map(
                        positions -> {
                            meterRegistry
                                    .counter("positions_cache_requests_total", "result", "hit")
                                    .increment();
                            return positions;
                        })
                .orElseGet(this::loadAndCachePositions);
    }

    public int warmUpCache() {
        return getPositions().size();
    }

    public boolean hasCachedPositions() {
        return positionCacheRepository.existsAll();
    }

    public boolean evictCachedPositions() {
        return positionCacheRepository.evictAll();
    }

    private List<PositionResponse> loadAndCachePositions() {
        meterRegistry.counter("positions_cache_requests_total", "result", "miss").increment();

        synchronized (cacheLoadMonitor) {
            return positionCacheRepository
                    .findAll()
                    .orElseGet(
                            () -> {
                                long startedAtNanos = System.nanoTime();
                                try {
                                    List<PositionResponse> positions =
                                            positionRepository
                                                    .findAll(Sort.by(Sort.Direction.ASC, "id"))
                                                    .stream()
                                                    .map(positionMapper::toResponse)
                                                    .toList();
                                    positionCacheRepository.saveAll(positions);
                                    meterRegistry
                                            .counter(
                                                    "positions_cache_write_total",
                                                    "result",
                                                    "success")
                                            .increment();
                                    recordLoadDuration(startedAtNanos, "success");
                                    return positions;
                                } catch (RuntimeException ex) {
                                    meterRegistry
                                            .counter(
                                                    "positions_cache_write_total",
                                                    "result",
                                                    "failed")
                                            .increment();
                                    recordLoadDuration(startedAtNanos, "failed");
                                    throw ex;
                                }
                            });
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
