package com.sipomeokjo.commitme.domain.refreshToken.repository;

import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query(
            """
            select count(token)
              from RefreshToken token
             where token.user.id = :userId
               and token.revokedAt is null
               and token.expiresAt > :now
            """)
    int countActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.user.id = :userId
               and token.revokedAt is null
            """)
    void revokeAllByUserId(
            @Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
