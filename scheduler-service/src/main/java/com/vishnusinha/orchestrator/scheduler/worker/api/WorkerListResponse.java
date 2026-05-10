package com.vishnusinha.orchestrator.scheduler.worker.api;

import java.util.List;

public record WorkerListResponse(
        List<WorkerResponse> workers,
        int totalWorkers,
        int activeWorkers,
        int unhealthyWorkers
) {
}
