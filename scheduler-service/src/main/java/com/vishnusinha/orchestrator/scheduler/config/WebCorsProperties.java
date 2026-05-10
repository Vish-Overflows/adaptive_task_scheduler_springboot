package com.vishnusinha.orchestrator.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "orchestrator.web")
public record WebCorsProperties(List<String> allowedOrigins) {

    public WebCorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://localhost:8080"
            );
        }
    }
}
