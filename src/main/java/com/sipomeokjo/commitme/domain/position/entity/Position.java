package com.sipomeokjo.commitme.domain.position.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "positions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Builder
    public Position(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
