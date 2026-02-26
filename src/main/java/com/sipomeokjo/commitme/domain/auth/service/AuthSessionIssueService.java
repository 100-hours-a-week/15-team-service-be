package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheService;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import com.sipomeokjo.commitme.security.jwt.RefreshTokenProvider;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthSessionIssueService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheService refreshTokenCacheService;
    private final UserRepository userRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthTokenReissueResult issueTokens(Long userId, UserStatus status) {
        String accessToken = accessTokenProvider.createAccessToken(userId, status);
        String refreshToken = refreshTokenProvider.generateRawToken();
        String refreshTokenHash = refreshTokenProvider.hash(refreshToken);

        Instant refreshExpiresAt = Instant.now(clock).plus(jwtProperties.getRefreshExpiration());
        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .user(userRepository.getReferenceById(userId))
                        .tokenHash(refreshTokenHash)
                        .expiresAt(refreshExpiresAt)
                        .revokedAt(null)
                        .build();
        refreshTokenRepository.save(refreshTokenEntity);
        refreshTokenCacheService.cache(refreshTokenHash, userId, status, refreshExpiresAt);

        return new AuthTokenReissueResult(accessToken, refreshToken);
    }

    public boolean revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return false;
        }

        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);
        int revoked = refreshTokenRepository.revokeByTokenHash(tokenHash, Instant.now(clock));
        refreshTokenCacheService.evict(tokenHash);
        return revoked > 0;
    }
}
