package com.sipomeokjo.commitme.domain.position.mapper;

import com.sipomeokjo.commitme.domain.position.dto.PositionResponse;
import com.sipomeokjo.commitme.domain.position.entity.Position;
import org.springframework.stereotype.Component;

@Component
public class PositionMapper {

    public PositionResponse toResponse(Position position) {
        if (position == null) {
            return null;
        }
        return new PositionResponse(position.getId(), position.getName());
    }
}
