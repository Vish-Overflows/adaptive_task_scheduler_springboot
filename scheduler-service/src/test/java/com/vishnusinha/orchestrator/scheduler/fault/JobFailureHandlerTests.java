package com.vishnusinha.orchestrator.scheduler.fault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.config.FaultToleranceProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JobFailureHandlerTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-05T10:00:00Z"), ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrchestratorMetrics orchestratorMetrics = mock(OrchestratorMetrics.class);

    @Test
    void retriesJobWhenRetryBudgetRemains() {
        JobEntity job = queuedJob();
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);

        JobFailureHandler handler = new JobFailureHandler(
                new FaultToleranceProperties(true, Duration.ofSeconds(60), 5000, 3),
                orchestratorMetrics,
                CLOCK
        );

        handler.retryOrFail(job, "worker unhealthy");

        assertThat(job.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(job.getAssignedWorkerId()).isNull();
        assertThat(job.getRetryCount()).isEqualTo(1);
    }

    @Test
    void permanentlyFailsJobWhenRetryBudgetIsExhausted() {
        JobEntity job = queuedJob();
        job.retryFromFailure(CLOCK);
        job.retryFromFailure(CLOCK);
        job.retryFromFailure(CLOCK);
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);

        JobFailureHandler handler = new JobFailureHandler(
                new FaultToleranceProperties(true, Duration.ofSeconds(60), 5000, 3),
                orchestratorMetrics,
                CLOCK
        );

        handler.retryOrFail(job, "worker unhealthy");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getRetryCount()).isEqualTo(3);
        assertThat(job.getCompletedAt()).isEqualTo(Instant.parse("2026-05-05T10:00:00Z"));
    }

    private JobEntity queuedJob() {
        return JobEntity.queued(
                "CPU_BENCHMARK",
                100,
                objectMapper.createObjectNode().put("inputSize", 128),
                1000,
                null,
                CLOCK
        );
    }
}
