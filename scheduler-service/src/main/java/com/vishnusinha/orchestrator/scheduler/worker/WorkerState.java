package com.vishnusinha.orchestrator.scheduler.worker;

import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;

import java.net.URI;
import java.time.Instant;

public record WorkerState(
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
    public WorkerState withHeartbeat(int activeJobCount, int maxConcurrentJobs, double loadScore, Instant now) {
        return new WorkerState(
                workerId,
                serviceName,
                version,
                environment,
                baseUrl,
                maxConcurrentJobs,
                activeJobCount,
                loadScore,
                WorkerStatus.ACTIVE,
                registeredAt,
                now,
                now
        );
    }

    public WorkerState withStatus(WorkerStatus status, Instant now) {
        return new WorkerState(
                workerId,
                serviceName,
                version,
                environment,
                baseUrl,
                maxConcurrentJobs,
                activeJobCount,
                loadScore,
                status,
                registeredAt,
                lastHeartbeatAt,
                now
        );
    }
}
