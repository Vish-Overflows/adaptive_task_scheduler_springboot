package com.vishnusinha.orchestrator.scheduler.job.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String type,
        int priority,
        JsonNode payload,
        long estimatedDurationMs,
        JobStatus status,
        String assignedWorkerId,
        String scheduledPolicy,
        int retryCount,
        String idempotencyKey,
        Instant createdAt,
        Instant queuedAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
