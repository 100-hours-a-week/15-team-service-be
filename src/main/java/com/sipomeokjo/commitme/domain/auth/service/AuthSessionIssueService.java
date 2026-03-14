package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.refreshToken.service.RefreshTokenCacheAfterCommitService;
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
import org.springframework.transaction.support.TransactionOperations;

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
    private final TransactionOperations transactionOperations;

    public AuthTokenReissueResult rotateTokens(Long userId, UserStatus status) {
        PreparedTokenIssue preparedTokenIssue = prepareTokenIssue(userId, status);
        AuthTokenReissueResult result =
                transactionOperations.execute(
                        transactionStatus -> {
                            Instant now = Instant.now(clock);
                            List<String> activeTokenHashes =
                                    refreshTokenRepository.findActiveTokenHashesByUserId(userId);
                            if (!activeTokenHashes.isEmpty()) {
                                refreshTokenRepository.revokeAllByUserId(userId, now);
                                refreshTokenCacheAfterCommitService.evictAll(activeTokenHashes);
                            }
                            persistIssuedRefreshToken(preparedTokenIssue);
                            return preparedTokenIssue.toResult();
                        });
        return requireResult(result);
    }

    public AuthTokenReissueResult issueTokens(Long userId, UserStatus status) {
        PreparedTokenIssue preparedTokenIssue = prepareTokenIssue(userId, status);
        AuthTokenReissueResult result =
                transactionOperations.execute(
                        transactionStatus -> {
                            persistIssuedRefreshToken(preparedTokenIssue);
                            return preparedTokenIssue.toResult();
                        });
        return requireResult(result);
    }

    public AuthTokenReissueResult reissueTokens(String tokenHash, Long userId, UserStatus status) {
        PreparedTokenIssue preparedTokenIssue = prepareTokenIssue(userId, status);
        AuthTokenReissueResult result =
                transactionOperations.execute(
                        transactionStatus -> {
                            int revoked =
                                    refreshTokenRepository.revokeByTokenHash(
                                            tokenHash, Instant.now(clock));
                            if (revoked <= 0) {
                                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
                            }

                            refreshTokenCacheAfterCommitService.evict(tokenHash);
                            persistIssuedRefreshToken(preparedTokenIssue);
                            return preparedTokenIssue.toResult();
                        });
        return requireResult(result);
    }

    public AuthTokenReissueResult reissueTokens(String tokenHash) {
        Instant now = Instant.now(clock);
        var refreshTokenSource =
                refreshTokenRepository
                        .findReissueSourceByTokenHash(tokenHash)
                        .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshTokenSource.getRevokedAt() != null
                || !refreshTokenSource.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        PreparedTokenIssue preparedTokenIssue =
                prepareTokenIssue(
                        refreshTokenSource.getUserId(), refreshTokenSource.getUserStatus());
        AuthTokenReissueResult result =
                transactionOperations.execute(
                        transactionStatus -> {
                            int revoked =
                                    refreshTokenRepository.revokeByTokenHash(
                                            tokenHash, Instant.now(clock));
                            if (revoked <= 0) {
                                throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
                            }

                            refreshTokenCacheAfterCommitService.evict(tokenHash);
                            persistIssuedRefreshToken(preparedTokenIssue);
                            return preparedTokenIssue.toResult();
                        });
        return requireResult(result);
    }

    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = refreshTokenProvider.hash(rawRefreshToken);
        transactionOperations.executeWithoutResult(
                transactionStatus -> {
                    int revoked =
                            refreshTokenRepository.revokeByTokenHash(tokenHash, Instant.now(clock));
                    if (revoked > 0) {
                        refreshTokenCacheAfterCommitService.evict(tokenHash);
                    }
                });
    }

    private PreparedTokenIssue prepareTokenIssue(Long userId, UserStatus status) {
        String accessToken = accessTokenProvider.createAccessToken(userId, status);
        String refreshToken = refreshTokenProvider.generateRawToken();
        String refreshTokenHash = refreshTokenProvider.hash(refreshToken);
        Instant refreshExpiresAt = Instant.now(clock).plus(jwtProperties.getRefreshExpiration());
        return new PreparedTokenIssue(
                userId, status, accessToken, refreshToken, refreshTokenHash, refreshExpiresAt);
    }

    private void persistIssuedRefreshToken(PreparedTokenIssue preparedTokenIssue) {
        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .user(userRepository.getReferenceById(preparedTokenIssue.userId()))
                        .tokenHash(preparedTokenIssue.refreshTokenHash())
                        .expiresAt(preparedTokenIssue.refreshExpiresAt())
                        .revokedAt(null)
                        .build();
        refreshTokenRepository.save(refreshTokenEntity);
        refreshTokenCacheAfterCommitService.cache(
                preparedTokenIssue.refreshTokenHash(),
                preparedTokenIssue.userId(),
                preparedTokenIssue.status(),
                preparedTokenIssue.refreshExpiresAt());
    }

    private AuthTokenReissueResult requireResult(AuthTokenReissueResult result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return result;
    }

    private record PreparedTokenIssue(
            Long userId,
            UserStatus status,
            String accessToken,
            String refreshToken,
            String refreshTokenHash,
            Instant refreshExpiresAt) {

        private AuthTokenReissueResult toResult() {
            return new AuthTokenReissueResult(accessToken, refreshToken);
        }
    }
}
