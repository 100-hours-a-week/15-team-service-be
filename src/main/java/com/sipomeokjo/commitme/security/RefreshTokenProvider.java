package com.sipomeokjo.commitme.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenProvider {
	
	private static final int TOKEN_BYTES = 32;
	
	private final SecureRandom secureRandom = new SecureRandom();
	private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
	
	public String generateRawToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		
		return base64UrlEncoder.encodeToString(bytes);
	}
	
	public String hash(String refreshToken) {
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not supported", e);
		}
	}
	
	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
			sb.append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}
}
