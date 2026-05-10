package com.vishnusinha.orchestrator.worker.runtime;

import com.vishnusinha.orchestrator.worker.config.WorkerServiceProperties;
import org.springframework.stereotype.Component;

@Component
public class WorkerLoadSampler {

    private final WorkerServiceProperties properties;
    private final WorkerExecutionService workerExecutionService;

    public WorkerLoadSampler(WorkerServiceProperties properties, WorkerExecutionService workerExecutionService) {
        this.properties = properties;
        this.workerExecutionService = workerExecutionService;
    }

    public WorkerRuntimeSnapshot snapshot() {
        int activeJobCount = workerExecutionService.activeJobCount();
        double loadScore = activeJobCount / (double) properties.maxConcurrentJobs();
        return new WorkerRuntimeSnapshot(
                activeJobCount,
                properties.maxConcurrentJobs(),
                loadScore
        );
    }
}
