package com.sipomeokjo.commitme.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.csrf")
public record CsrfProperties(String cookieDomain) {}
