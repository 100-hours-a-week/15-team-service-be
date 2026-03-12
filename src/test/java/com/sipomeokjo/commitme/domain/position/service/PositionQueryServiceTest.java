package com.sipomeokjo.commitme.domain.position.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sipomeokjo.commitme.config.PositionCacheConfig;
import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.mapper.PositionMapper;
import com.sipomeokjo.commitme.domain.position.repository.PositionCacheRepository;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PositionQueryServiceTest {

    @Mock private PositionRepository positionRepository;
    @Mock private PositionMapper positionMapper;

    private PositionQueryService positionQueryService;

    @BeforeEach
    void setUp() {
        CacheManager localCacheManager = new PositionCacheConfig().localCacheManager();
        PositionCacheRepository positionCacheRepository =
                new PositionCacheRepository(localCacheManager);
        positionQueryService =
                new PositionQueryService(
                        positionRepository,
                        positionMapper,
                        positionCacheRepository,
                        new SimpleMeterRegistry());
    }

    @Test
    void getPositions_cacheMissThenHit_repositoryLoadsOnlyOnce() {
        Position backend = Position.builder().id(1L).name("백엔드").build();
        Position frontend = Position.builder().id(2L).name("프론트엔드").build();
        PositionResponse backendResponse = new PositionResponse(1L, "백엔드");
        PositionResponse frontendResponse = new PositionResponse(2L, "프론트엔드");

        given(positionRepository.findAll(any(Sort.class))).willReturn(List.of(backend, frontend));
        given(positionMapper.toResponse(backend)).willReturn(backendResponse);
        given(positionMapper.toResponse(frontend)).willReturn(frontendResponse);

        List<PositionResponse> first = positionQueryService.getPositions();
        List<PositionResponse> second = positionQueryService.getPositions();

        assertThat(first).containsExactly(backendResponse, frontendResponse);
        assertThat(second).containsExactly(backendResponse, frontendResponse);
        assertThat(positionQueryService.hasCachedPositions()).isTrue();
        verify(positionRepository, times(1)).findAll(any(Sort.class));
    }
}
