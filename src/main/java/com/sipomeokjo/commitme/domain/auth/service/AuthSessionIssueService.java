package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheAfterCommitService;
import com.sipomeokjo.commitme.domain.user.entity.User;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import com.sipomeokjo.commitme.security.jwt.RefreshTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthSessionIssueService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCacheAfterCommitService refreshTokenCacheAfterCommitService;
    private final UserRepository userRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public AuthTokenReissueResult rotateTokens(Long userId, UserStatus status) {
        Instant now = Instant.now(clock);
        List<String> activeTokenHashes =
                refreshTokenRepository.findActiveTokenHashesByUserId(userId);
        if (!activeTokenHashes.isEmpty()) {
            refreshTokenRepository.revokeAllByUserId(userId, now);
            refreshTokenCacheAfterCommitService.evictAll(activeTokenHashes);
        }
        return issueTokensInternal(userId, status);
    }

    @Transactional
    public AuthTokenReissueResult issueTokens(Long userId, UserStatus status) {
        return issueTokensInternal(userId, status);
    }

    @Transactional
    public AuthTokenReissueResult reissueTokens(String tokenHash, Long userId) {
        int revoked = refreshTokenRepository.revokeByTokenHash(tokenHash, Instant.now(clock));
        if (revoked <= 0) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshTokenCacheAfterCommitService.evict(tokenHash);
        UserStatus status =
                userRepository
                        .findById(userId)
                        .map(User::getStatus)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        return issueTokensInternal(userId, status);
    }

    @Transactional
    public AuthTokenReissueResult reissueTokens(String tokenHash) {
        Instant now = Instant.now(clock);
        RefreshToken refreshTokenEntity =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshTokenEntity.getRevokedAt() != null
                || !refreshTokenEntity.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        var user = refreshTokenEntity.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        refreshTokenEntity.revoke(now);
        refreshTokenCacheAfterCommitService.evict(tokenHash);
        return issueTokensInternal(user.getId(), user.getStatus());
    }

    private AuthTokenReissueResult issueTokensInternal(Long userId, UserStatus status) {
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
        refreshTokenCacheAfterCommitService.cache(
                refreshTokenHash, userId, status, refreshExpiresAt);

        return new AuthTokenReissueResult(accessToken, refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);
        int revoked = refreshTokenRepository.revokeByTokenHash(tokenHash, Instant.now(clock));
        if (revoked > 0) {
            refreshTokenCacheAfterCommitService.evict(tokenHash);
        }
    }
}
