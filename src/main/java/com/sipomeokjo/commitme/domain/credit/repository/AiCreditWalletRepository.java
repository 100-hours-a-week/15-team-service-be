package com.sipomeokjo.commitme.domain.credit.repository;

import com.sipomeokjo.commitme.domain.credit.entity.AiCreditWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiCreditWalletRepository extends JpaRepository<AiCreditWallet, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE AiCreditWallet w
               SET w.balance = w.balance - :amount
             WHERE w.userId = :userId
               AND w.balance >= :amount
            """)
    int deductIfSufficient(@Param("userId") Long userId, @Param("amount") long amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE AiCreditWallet w
               SET w.balance = w.balance + :amount
             WHERE w.userId = :userId
            """)
    int refund(@Param("userId") Long userId, @Param("amount") long amount);
}
