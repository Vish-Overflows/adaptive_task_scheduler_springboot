package com.vishnusinha.orchestrator.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator.queues")
public record QueueProperties(
        String pendingJobsKey
) {
}
