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


    public void update(String name, String preferred, String idealTalent) {
        if (name != null && !name.isBlank()) this.name = name;
        if (preferred != null) this.preferred = preferred;
        if (idealTalent != null) this.idealTalent = idealTalent;
    }

    public void verify(boolean verified) {
        this.isVerified = verified;
    }

    public static Company create(String name, String preferred, String idealTalent) {
        Company c = new Company();
        c.name = name;
        c.preferred = preferred;
        c.idealTalent = idealTalent;
        c.isVerified = false;
        return c;
    }

}
