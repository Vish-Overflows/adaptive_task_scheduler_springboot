package com.vishnusinha.orchestrator.worker.runtime;

import com.vishnusinha.orchestrator.shared.job.CompleteJobRequest;
import com.vishnusinha.orchestrator.shared.job.DispatchJobRequest;
import com.vishnusinha.orchestrator.shared.job.JobExecutionStatus;
import com.vishnusinha.orchestrator.worker.config.WorkerServiceProperties;
import com.vishnusinha.orchestrator.worker.workload.WorkloadCatalog;
import com.vishnusinha.orchestrator.worker.workload.WorkloadResult;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkerExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkerExecutionService.class);

    private final WorkerServiceProperties properties;
    private final WebClient schedulerWebClient;
    private final WorkloadCatalog workloadCatalog;
    private final Clock clock;
    private final ExecutorService executorService;
    private final Semaphore permits;
    private final AtomicInteger activeJobCount = new AtomicInteger(0);

    public WorkerExecutionService(
            WorkerServiceProperties properties,
            WebClient schedulerWebClient,
            WorkloadCatalog workloadCatalog,
            Clock clock
    ) {
        this.properties = properties;
        this.schedulerWebClient = schedulerWebClient;
        this.workloadCatalog = workloadCatalog;
        this.clock = clock;
        this.executorService = Executors.newFixedThreadPool(properties.maxConcurrentJobs());
        this.permits = new Semaphore(properties.maxConcurrentJobs());
    }

    public void accept(DispatchJobRequest request) {
        if (!permits.tryAcquire()) {
            throw new WorkerCapacityExceededException(
                    "Worker %s has no available execution slots".formatted(properties.workerId())
            );
        }

        activeJobCount.incrementAndGet();
        executorService.submit(() -> execute(request));
    }

    public int activeJobCount() {
        return activeJobCount.get();
    }

    private void execute(DispatchJobRequest request) {
        Instant startedAt = Instant.now(clock);
        JobExecutionStatus status = JobExecutionStatus.COMPLETED;
        String message = "Job completed";

        try {
            WorkloadResult result = workloadCatalog.execute(request.type(), request.payload());
            message = "%s; operations=%d".formatted(result.summary(), result.operations());
            log.info("Worker {} completed job {}: {}", properties.workerId(), request.jobId(), message);
        } catch (RuntimeException exception) {
            status = JobExecutionStatus.FAILED;
            message = exception.getMessage();
            log.warn("Worker {} failed job {}", properties.workerId(), request.jobId());
        } finally {
            long runtimeMs = Duration.between(startedAt, Instant.now(clock)).toMillis();
            reportCompletion(request, status, message, runtimeMs);
            activeJobCount.decrementAndGet();
            permits.release();
        }
    }

    private void reportCompletion(
            DispatchJobRequest request,
            JobExecutionStatus status,
            String message,
            long runtimeMs
    ) {
        CompleteJobRequest completion = new CompleteJobRequest(
                request.jobId(),
                properties.workerId(),
                status,
                message,
                Math.max(runtimeMs, 0)
        );

        try {
            schedulerWebClient.post()
                    .uri("/api/v1/jobs/{jobId}/completion", request.jobId())
                    .bodyValue(completion)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (RuntimeException exception) {
            log.warn("Worker {} could not report completion for job {}", properties.workerId(), request.jobId());
        }
    }

    @PreDestroy
    void shutdown() {
        executorService.shutdownNow();
    }
}
