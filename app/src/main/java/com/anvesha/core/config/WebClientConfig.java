package com.anvesha.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {

    @Bean(name = "chemServiceWebClient")
    public WebClient chemServiceWebClient(
            @Value("${anvesha.python-service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB
                .build();
    }

    @Bean(name = "ragServiceWebClient")
    public WebClient ragServiceWebClient(
            @Value("${anvesha.rag-service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50 MB
                .build();
    }
}
