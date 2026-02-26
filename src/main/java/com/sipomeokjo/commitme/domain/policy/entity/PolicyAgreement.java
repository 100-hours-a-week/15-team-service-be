package com.sipomeokjo.commitme.domain.policy.entity;

import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Getter
@Entity
@Builder
@Table(name = "policy_agreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
    private Instant agreedAt;
}
