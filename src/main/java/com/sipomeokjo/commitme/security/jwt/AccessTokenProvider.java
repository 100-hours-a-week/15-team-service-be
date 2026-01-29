package com.sipomeokjo.commitme.security.jwt;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenProvider {

    private final JwtProperties jwtProperties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Clock clock;

    private static final String CLAIM_STATUS = "status";

    public String createAccessToken(long userId, UserStatus status) {
        Instant now = Instant.now(clock);

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuedAt(now)
                        .expiresAt(now.plus(jwtProperties.getAccessExpiration()))
                        .subject(String.valueOf(userId))
                        .claim(CLAIM_STATUS, status.name())
                        .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    private Jwt parseClaims(String token) {
        return jwtDecoder.decode(token);
    }

    public Long getUserId(String token) {
        Jwt jwt = parseClaims(token);
        return Long.parseLong(jwt.getSubject());
    }

    public String getStatus(String token) {
        Jwt jwt = parseClaims(token);
        Object status = jwt.getClaims().get(CLAIM_STATUS);
        return status == null ? null : status.toString();
    }

    public Instant getIssuedAt(String token) {
        Jwt jwt = parseClaims(token);
        return jwt.getIssuedAt();
    }

    public Instant getExpiredAt(String token) {
        Jwt jwt = parseClaims(token);
        return jwt.getExpiresAt();
    }

    public boolean validateToken(String accessToken) {
        try {
            return !getExpiredAt(accessToken).isBefore(Instant.now(clock));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String accessToken) {
        Long userId = getUserId(accessToken);
        String status = getStatus(accessToken);
        if (status == null || status.isBlank()) {
            return new UsernamePasswordAuthenticationToken(userId, accessToken, List.of());
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + status));

        return new UsernamePasswordAuthenticationToken(userId, accessToken, authorities);
    }
}
