package com.vishnusinha.orchestrator.scheduler.api;

import com.vishnusinha.orchestrator.scheduler.config.SchedulerServiceProperties;
import com.vishnusinha.orchestrator.shared.ServiceRole;
import com.vishnusinha.orchestrator.shared.api.ServiceInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class ServiceInfoController {

    private final SchedulerServiceProperties properties;
    private final Clock clock;

    public ServiceInfoController(SchedulerServiceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping("/info")
    public ServiceInfoResponse info() {
        return new ServiceInfoResponse(
                properties.serviceName(),
                ServiceRole.SCHEDULER,
                properties.version(),
                properties.environment(),
                Instant.now(clock)
        );
    }
}
