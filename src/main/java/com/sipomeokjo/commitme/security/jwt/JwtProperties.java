package com.sipomeokjo.commitme.security.jwt;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secret;
    private Duration accessExpiration;
    private Duration refreshExpiration;
}
