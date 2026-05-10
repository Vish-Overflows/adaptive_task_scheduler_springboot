package com.vishnusinha.orchestrator.scheduler.fault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.config.FaultToleranceProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobReclaimerTests {

    private static final Instant NOW = Instant.parse("2026-05-05T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final WorkerRegistry workerRegistry = mock(WorkerRegistry.class);
    private final JobFailureHandler jobFailureHandler = mock(JobFailureHandler.class);
    private final OrchestratorMetrics orchestratorMetrics = mock(OrchestratorMetrics.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobReclaimer jobReclaimer = new JobReclaimer(
            new FaultToleranceProperties(true, Duration.ofSeconds(30), 5000, 3),
            jobRepository,
            workerRegistry,
            jobFailureHandler,
            orchestratorMetrics,
            CLOCK
    );

    @Test
    void reclaimsJobAssignedToUnhealthyWorker() {
        JobEntity job = runningJob("worker-1", NOW.minusSeconds(5));
        when(workerRegistry.list()).thenReturn(List.of(worker("worker-1", WorkerStatus.UNHEALTHY)));
        when(jobRepository.findByStatusIn(anyCollection())).thenReturn(List.of(job));

        int reclaimed = jobReclaimer.reclaimUnsafeJobs();

        assertThat(reclaimed).isEqualTo(1);
        verify(jobFailureHandler).retryOrFail(job, "assigned worker is unhealthy");
    }

    @Test
    void reclaimsJobThatExceededInFlightTimeout() {
        JobEntity job = runningJob("worker-1", NOW.minusSeconds(60));
        when(workerRegistry.list()).thenReturn(List.of(worker("worker-1", WorkerStatus.ACTIVE)));
        when(jobRepository.findByStatusIn(anyCollection())).thenReturn(List.of(job));

        int reclaimed = jobReclaimer.reclaimUnsafeJobs();

        assertThat(reclaimed).isEqualTo(1);
        verify(jobFailureHandler).retryOrFail(job, "job exceeded in-flight timeout of PT30S");
    }

    private JobEntity runningJob(String workerId, Instant startedAt) {
        JobEntity job = JobEntity.queued(
                "CPU_BENCHMARK",
                100,
                objectMapper.createObjectNode().put("inputSize", 128),
                1000,
                null,
                Clock.fixed(startedAt, ZoneOffset.UTC)
        );
        job.markScheduled(workerId, Clock.fixed(startedAt, ZoneOffset.UTC));
        job.markRunning(Clock.fixed(startedAt, ZoneOffset.UTC));
        return job;
    }

    private static WorkerState worker(String workerId, WorkerStatus status) {
        return new WorkerState(
                workerId,
                "worker-service",
                "0.1.0",
                "test",
                URI.create("http://" + workerId + ":8081"),
                4,
                0,
                0.0,
                status,
                NOW,
                NOW,
                NOW
        );
    }
}
