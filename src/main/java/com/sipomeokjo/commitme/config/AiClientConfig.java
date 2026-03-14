package com.sipomeokjo.commitme.config;

import com.sipomeokjo.commitme.domain.resume.config.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    @Bean
    RestClient aiClient(RestClient.Builder builder, AiProperties aiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(aiProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(aiProperties.getReadTimeoutMs());
        return builder.requestFactory(requestFactory).build();
    }
}
