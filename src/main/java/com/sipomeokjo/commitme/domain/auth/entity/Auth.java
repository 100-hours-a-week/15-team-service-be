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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

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

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "token_scopes", columnDefinition = "json")
    private String tokenScopes;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Builder
    public Auth(Long id, User user, AuthProvider provider, String providerUserId, String providerUsername, String accessToken, String tokenScopes, LocalDateTime tokenExpiresAt) {
        this.id = id;
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerUsername = providerUsername;
        this.accessToken = accessToken;
        this.tokenScopes = tokenScopes;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public void updateTokenInfo(String providerUsername, String accessToken, String tokenScopes, LocalDateTime tokenExpiresAt) {
        this.providerUsername = providerUsername;
        this.accessToken = accessToken;
        this.tokenScopes = tokenScopes;
        this.tokenExpiresAt = tokenExpiresAt;
    }
}
