package com.vishnusinha.orchestrator.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator.scheduler")
public record SchedulerServiceProperties(
        String serviceName,
        String version,
        String environment
) {
}
