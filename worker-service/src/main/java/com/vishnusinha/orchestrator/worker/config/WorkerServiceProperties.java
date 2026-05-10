package com.vishnusinha.orchestrator.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "orchestrator.worker")
public record WorkerServiceProperties(
        String serviceName,
        String workerId,
        String version,
        String environment,
        URI schedulerBaseUrl,
        URI publicBaseUrl,
        long heartbeatIntervalMs,
        int maxConcurrentJobs,
        boolean registrationEnabled
) {
}
