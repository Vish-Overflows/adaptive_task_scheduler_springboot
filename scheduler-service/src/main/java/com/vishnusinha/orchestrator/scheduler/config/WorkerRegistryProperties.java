package com.vishnusinha.orchestrator.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "orchestrator.workers")
public record WorkerRegistryProperties(
        String registryKey,
        Duration heartbeatTimeout
) {
}
