package com.vishnusinha.orchestrator.worker.scheduler;

import com.vishnusinha.orchestrator.shared.worker.RegisterWorkerRequest;
import com.vishnusinha.orchestrator.shared.worker.WorkerHeartbeatRequest;
import com.vishnusinha.orchestrator.worker.config.WorkerServiceProperties;
import com.vishnusinha.orchestrator.worker.runtime.WorkerLoadSampler;
import com.vishnusinha.orchestrator.worker.runtime.WorkerRuntimeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicBoolean;

@ConditionalOnProperty(prefix = "orchestrator.worker", name = "registration-enabled", havingValue = "true", matchIfMissing = true)
@Component
public class SchedulerRegistrationClient {

    private static final Logger log = LoggerFactory.getLogger(SchedulerRegistrationClient.class);

    private final WorkerServiceProperties properties;
    private final WorkerLoadSampler loadSampler;
    private final WebClient schedulerWebClient;
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public SchedulerRegistrationClient(
            WorkerServiceProperties properties,
            WorkerLoadSampler loadSampler,
            WebClient schedulerWebClient
    ) {
        this.properties = properties;
        this.loadSampler = loadSampler;
        this.schedulerWebClient = schedulerWebClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        attemptRegistration();
    }

    @Scheduled(
            initialDelayString = "${orchestrator.worker.heartbeat-interval-ms:5000}",
            fixedDelayString = "${orchestrator.worker.heartbeat-interval-ms:5000}"
    )
    public void heartbeat() {
        if (!registered.get()) {
            attemptRegistration();
            return;
        }

        WorkerRuntimeSnapshot snapshot = loadSampler.snapshot();
        WorkerHeartbeatRequest request = new WorkerHeartbeatRequest(
                properties.workerId(),
                snapshot.activeJobCount(),
                snapshot.maxConcurrentJobs(),
                snapshot.loadScore()
        );

        try {
            schedulerWebClient.post()
                    .uri("/api/v1/workers/heartbeat")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (RuntimeException exception) {
            registered.set(false);
            log.warn("Heartbeat failed for worker {}. Will re-register on next tick.", properties.workerId());
        }
    }

    private void attemptRegistration() {
        RegisterWorkerRequest request = new RegisterWorkerRequest(
                properties.workerId(),
                properties.serviceName(),
                properties.version(),
                properties.environment(),
                properties.publicBaseUrl(),
                properties.maxConcurrentJobs()
        );

        try {
            schedulerWebClient.post()
                    .uri("/api/v1/workers/register")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            registered.set(true);
            log.info("Registered worker {} with scheduler at {}", properties.workerId(), properties.schedulerBaseUrl());
        } catch (RuntimeException exception) {
            registered.set(false);
            log.warn("Worker {} could not register with scheduler at {} yet.", properties.workerId(), properties.schedulerBaseUrl());
        }
    }
}
