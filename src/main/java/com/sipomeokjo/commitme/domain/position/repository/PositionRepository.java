package com.sipomeokjo.commitme.domain.position.repository;

import com.sipomeokjo.commitme.domain.position.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}
