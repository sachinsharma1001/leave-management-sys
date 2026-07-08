package com.assignment.leave.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.openai")
    OpenAiProperties openAiProperties() {
        return new OpenAiProperties();
    }

    @Bean
    RestClient openAiRestClient(OpenAiProperties properties, RestClient.Builder builder) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Data
    public static class OpenAiProperties {
        private String apiKey;
        private String baseUrl = "https://api.openai.com";
        private String model = "gpt-4.1-mini";
        private Duration timeout = Duration.ofSeconds(30);
    }
}
