package com.vishnusinha.orchestrator.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient schedulerWebClient(WorkerServiceProperties properties, WebClient.Builder builder) {
        return builder
                .baseUrl(properties.schedulerBaseUrl().toString())
                .build();
    }
}
