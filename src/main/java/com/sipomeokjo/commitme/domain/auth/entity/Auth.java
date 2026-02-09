package com.sipomeokjo.commitme.domain.auth.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_username")
    private String providerUsername;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "token_scopes", columnDefinition = "TEXT")
    private String tokenScopes;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Builder
    public Auth(
            Long id,
            User user,
            AuthProvider provider,
            String providerUserId,
            String providerUsername,
            String accessToken,
            String tokenScopes,
            Instant tokenExpiresAt) {
        this.id = id;
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerUsername = providerUsername;
        this.accessToken = accessToken;
        this.tokenScopes = tokenScopes;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void updateTokenInfo(
            String providerUsername,
            String accessToken,
            String tokenScopes,
            Instant tokenExpiresAt) {
        this.providerUsername = providerUsername;
        this.accessToken = accessToken;
        this.tokenScopes = tokenScopes;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void clearSensitiveInfo() {
        this.providerUsername = null;
        this.accessToken = null;
        this.tokenScopes = null;
        this.tokenExpiresAt = null;
    }
}
