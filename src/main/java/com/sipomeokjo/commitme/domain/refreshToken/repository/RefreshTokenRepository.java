package com.sipomeokjo.commitme.domain.refreshToken.repository;

import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	
	@Query(
            """
            select token.user.id as userId,
                   user.status as userStatus,
                   token.expiresAt as expiresAt,
                   token.revokedAt as revokedAt
              from RefreshToken token
              join token.user user
             where token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenReissueSourceView> findReissueSourceByTokenHash(
            @Param("tokenHash") String tokenHash);

    @Query(
            """
            select token.tokenHash
              from RefreshToken token
             where token.user.id = :userId
               and token.revokedAt is null
            """)
    List<String> findActiveTokenHashesByUserId(@Param("userId") Long userId);

    @Query(
            """
            select token.tokenHash
              from RefreshToken token
             where token.user.id in :userIds
            """)
    List<String> findTokenHashesByUserIds(@Param("userIds") List<Long> userIds);

    @Query(
            """
            select token
              from RefreshToken token
              join fetch token.user user
             where user.id in :userIds
               and token.revokedAt is null
            """)
    List<RefreshToken> findActiveTokensByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.user.id = :userId
               and token.revokedAt is null
            """)
    void revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update RefreshToken token
               set token.revokedAt = :revokedAt
             where token.tokenHash = :tokenHash
               and token.revokedAt is null
            """)
    int revokeByTokenHash(
            @Param("tokenHash") String tokenHash, @Param("revokedAt") Instant revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RefreshToken token where token.user.id in :userIds")
    int deleteAllByUserIds(@Param("userIds") List<Long> userIds);

    interface RefreshTokenReissueSourceView {
        Long getUserId();

        UserStatus getUserStatus();

        Instant getExpiresAt();

        Instant getRevokedAt();
    }
}
