package com.sipomeokjo.commitme.domain.company.entity;

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
@Table(name = "companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

    @Column(name = "preferred", columnDefinition = "TEXT")
    private String preferred;

    @Column(name = "ideal_talent", columnDefinition = "TEXT")
    private String idealTalent;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;
}
