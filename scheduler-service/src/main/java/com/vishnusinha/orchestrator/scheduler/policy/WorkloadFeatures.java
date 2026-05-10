package com.vishnusinha.orchestrator.scheduler.policy;

public record WorkloadFeatures(
        int queueDepth,
        double averageEstimatedDurationMs,
        double durationStdDevMs,
        double averagePriority,
        double priorityStdDev,
        int activeWorkerCount,
        double averageWorkerLoad,
        double workerLoadStdDev
) {
}
