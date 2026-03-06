package com.sipomeokjo.commitme.domain.user.entity;

import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tech_stacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechStack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 100, unique = true)
    private String nameNormalized;

    public static TechStack create(String name, String nameNormalized) {
        TechStack techStack = new TechStack();
        techStack.name = name;
        techStack.nameNormalized = nameNormalized;
        return techStack;
    }

    public void rename(String name, String nameNormalized) {
        this.name = name;
        this.nameNormalized = nameNormalized;
    }
}
