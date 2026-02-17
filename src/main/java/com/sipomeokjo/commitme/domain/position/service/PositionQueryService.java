package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.mapper.PositionMapper;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PositionQueryService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;

    @Cacheable(cacheNames = "positions", key = "'all'")
    public List<PositionResponse> getPositions() {
        return positionRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(positionMapper::toResponse)
                .toList();
    }
}
