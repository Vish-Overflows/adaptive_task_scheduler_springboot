package com.vishnusinha.orchestrator.scheduler.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.fault.JobFailureHandler;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.shared.job.CompleteJobRequest;
import com.vishnusinha.orchestrator.shared.job.JobExecutionStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCompletionServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-05T10:00:00Z"), ZoneOffset.UTC);

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final JobFailureHandler jobFailureHandler = mock(JobFailureHandler.class);
    private final OrchestratorMetrics orchestratorMetrics = mock(OrchestratorMetrics.class);
    private final JobCompletionService jobCompletionService = new JobCompletionService(
            jobRepository,
            jobFailureHandler,
            orchestratorMetrics,
            CLOCK
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completesRunningJobAssignedToReportingWorker() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = queuedJob(jobId);
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.saveAndFlush(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobEntity completed = jobCompletionService.complete(new CompleteJobRequest(
                jobId,
                "worker-1",
                JobExecutionStatus.COMPLETED,
                "ok",
                1000
        ));

        assertThat(completed.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isEqualTo(Instant.parse("2026-05-05T10:00:00Z"));
    }

    @Test
    void rejectsCompletionFromDifferentWorker() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = queuedJob(jobId);
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobCompletionService.complete(new CompleteJobRequest(
                jobId,
                "worker-2",
                JobExecutionStatus.COMPLETED,
                "wrong worker",
                1000
        ))).isInstanceOf(JobCompletionRejectedException.class);
    }

    @Test
    void duplicateCompletionFromAssignedWorkerIsIdempotent() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = queuedJob(jobId);
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);
        job.markCompleted(CLOCK);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        JobEntity completed = jobCompletionService.complete(new CompleteJobRequest(
                jobId,
                "worker-1",
                JobExecutionStatus.COMPLETED,
                "duplicate",
                1000
        ));

        assertThat(completed).isSameAs(job);
        verify(jobRepository, never()).saveAndFlush(any(JobEntity.class));
    }

    @Test
    void failedCompletionUsesRetryHandler() {
        UUID jobId = UUID.randomUUID();
        JobEntity job = queuedJob(jobId);
        job.markScheduled("worker-1", CLOCK);
        job.markRunning(CLOCK);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.saveAndFlush(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobCompletionService.complete(new CompleteJobRequest(
                jobId,
                "worker-1",
                JobExecutionStatus.FAILED,
                "simulated failure",
                1000
        ));

        verify(jobFailureHandler).retryOrFail(job, "worker reported failure: simulated failure");
    }

    private JobEntity queuedJob(UUID ignoredJobId) {
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
