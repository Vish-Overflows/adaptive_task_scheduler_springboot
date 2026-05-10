package com.vishnusinha.orchestrator.scheduler.metrics.api;

import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;

public record MetricsSummaryResponse(
        SchedulingPolicyType activePolicy,
        long queuedJobs,
        long scheduledJobs,
        long runningJobs,
        long completedJobs,
        long failedJobs,
        int totalWorkers,
        int activeWorkers,
        int unhealthyWorkers,
        double averageWorkerLoad
) {
}
