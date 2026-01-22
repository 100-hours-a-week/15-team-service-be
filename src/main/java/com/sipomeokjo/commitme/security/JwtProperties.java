package com.sipomeokjo.commitme.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
	private String secret;
	private Duration accessExpiration;
	private Duration refreshExpiration;
}
