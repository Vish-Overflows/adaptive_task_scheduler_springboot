package com.vishnusinha.orchestrator.worker.runtime;

public record WorkerRuntimeSnapshot(
        int activeJobCount,
        int maxConcurrentJobs,
        double loadScore
) {
}
