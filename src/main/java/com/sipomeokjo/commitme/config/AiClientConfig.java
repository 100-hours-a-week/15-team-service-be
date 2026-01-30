package com.sipomeokjo.commitme.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    @Bean
    RestClient aiClient(RestClient.Builder builder) {
        return builder.build();
    }
}
