package com.sipomeokjo.commitme.domain.policy.entity;

import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "policy_agreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "document", nullable = false, length = 1024)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyType policyType;

    @Column(name = "policy_version", nullable = false, length = 60)
    private String policyVersion;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

}
