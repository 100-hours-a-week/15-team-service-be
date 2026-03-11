package com.sipomeokjo.commitme.domain.position.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import com.sipomeokjo.commitme.domain.position.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionFinder {

    private final PositionRepository positionRepository;

    public Position getByIdOrThrow(Long positionId) {
        return positionRepository
                .findById(positionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POSITION_NOT_FOUND));
    }
}
