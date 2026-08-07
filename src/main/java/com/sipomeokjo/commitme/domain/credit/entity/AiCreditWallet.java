package com.sipomeokjo.commitme.domain.credit.entity;

import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_credit_wallet")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCreditWallet extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long balance;

    public static AiCreditWallet create(User user, long initialCredit) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (initialCredit < 0) {
            throw new IllegalArgumentException("initialCredit must not be negative");
        }

        AiCreditWallet wallet = new AiCreditWallet();
        wallet.user = user;
        wallet.balance = initialCredit;
        return wallet;
    }
}
