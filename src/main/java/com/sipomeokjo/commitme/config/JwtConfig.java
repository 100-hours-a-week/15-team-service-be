package com.sipomeokjo.commitme.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sipomeokjo.commitme.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {
	
	private final JwtProperties jwtProperties;
	
	@Bean
	public JwtEncoder jwtEncoder() {
		SecretKey key = new SecretKeySpec((jwtProperties.getSecret()).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		SecretKey key = new SecretKeySpec((jwtProperties.getSecret()).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(key).build();
	}
}
