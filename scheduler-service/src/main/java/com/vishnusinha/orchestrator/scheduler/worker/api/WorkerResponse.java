package com.vishnusinha.orchestrator.scheduler.worker.api;

import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;

import java.net.URI;
import java.time.Instant;

public record WorkerResponse(
        String workerId,
        String serviceName,
        String version,
        String environment,
        URI baseUrl,
        int maxConcurrentJobs,
        int activeJobCount,
        double loadScore,
        WorkerStatus status,
        Instant registeredAt,
        Instant lastHeartbeatAt,
        Instant updatedAt
) {
}
