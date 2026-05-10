package com.vishnusinha.orchestrator.scheduler.worker;

import com.vishnusinha.orchestrator.scheduler.worker.api.WorkerResponse;
import org.springframework.stereotype.Component;

@Component
public class WorkerMapper {

    public WorkerResponse toResponse(WorkerState worker) {
        return new WorkerResponse(
                worker.workerId(),
                worker.serviceName(),
                worker.version(),
                worker.environment(),
                worker.baseUrl(),
                worker.maxConcurrentJobs(),
                worker.activeJobCount(),
                worker.loadScore(),
                worker.status(),
                worker.registeredAt(),
                worker.lastHeartbeatAt(),
                worker.updatedAt()
        );
    }
}
