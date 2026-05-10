package com.vishnusinha.orchestrator.worker.workload;

public record WorkloadResult(
        String summary,
        long operations
) {
}
