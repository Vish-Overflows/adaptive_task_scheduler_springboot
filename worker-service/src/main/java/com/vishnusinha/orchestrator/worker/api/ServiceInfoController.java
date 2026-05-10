package com.vishnusinha.orchestrator.worker.api;

import com.vishnusinha.orchestrator.shared.ServiceRole;
import com.vishnusinha.orchestrator.worker.config.WorkerServiceProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class ServiceInfoController {

    private final WorkerServiceProperties properties;
    private final Clock clock;

    public ServiceInfoController(WorkerServiceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/info")
    public WorkerInfoResponse info() {
        return new WorkerInfoResponse(
                properties.serviceName(),
                ServiceRole.WORKER,
                properties.workerId(),
                properties.version(),
                properties.environment(),
                properties.schedulerBaseUrl(),
                Instant.now(clock)
        );
    }
}
