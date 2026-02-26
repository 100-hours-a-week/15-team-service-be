package com.sipomeokjo.commitme.domain.auth.service;

import com.sipomeokjo.commitme.domain.auth.dto.AuthTokenReissueResult;
import com.sipomeokjo.commitme.domain.refreshToken.entity.RefreshToken;
import com.sipomeokjo.commitme.domain.refreshToken.repository.RefreshTokenRepository;
import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import com.sipomeokjo.commitme.domain.user.repository.UserRepository;
import com.sipomeokjo.commitme.security.jwt.AccessTokenProvider;
import com.sipomeokjo.commitme.security.jwt.JwtProperties;
import com.sipomeokjo.commitme.security.jwt.RefreshTokenProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthSessionIssueService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthTokenReissueResult issueTokens(Long userId, UserStatus status) {
        String accessToken = accessTokenProvider.createAccessToken(userId, status);
        String refreshToken = refreshTokenProvider.generateRawToken();
        String refreshTokenHash = refreshTokenProvider.hash(refreshToken);

        LocalDateTime refreshExpiresAt = LocalDateTime.now(clock).plus(jwtProperties.getRefreshExpiration());
        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .user(userRepository.getReferenceById(userId))
                        .tokenHash(refreshTokenHash)
                        .expiresAt(refreshExpiresAt)
                        .revokedAt(null)
                        .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthTokenReissueResult(accessToken, refreshToken);
    }
}
