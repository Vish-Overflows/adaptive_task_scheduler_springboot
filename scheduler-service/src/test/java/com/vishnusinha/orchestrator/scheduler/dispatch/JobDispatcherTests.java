package com.vishnusinha.orchestrator.scheduler.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vishnusinha.orchestrator.scheduler.config.DispatchProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicy;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyResolver;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobDispatcherTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-05T10:00:00Z"), ZoneOffset.UTC);

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final WorkerRegistry workerRegistry = mock(WorkerRegistry.class);
    private final SchedulingPolicyResolver schedulingPolicyResolver = mock(SchedulingPolicyResolver.class);
    private final SchedulingPolicy schedulingPolicy = mock(SchedulingPolicy.class);
    private final WorkerDispatchClient workerDispatchClient = mock(WorkerDispatchClient.class);
    private final OrchestratorMetrics orchestratorMetrics = mock(OrchestratorMetrics.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JobDispatcher jobDispatcher = new JobDispatcher(
            new DispatchProperties(true, 1000, 1),
            jobRepository,
            workerRegistry,
            schedulingPolicyResolver,
            workerDispatchClient,
            orchestratorMetrics,
            CLOCK
    );

    @Test
    void dispatchesQueuedJobToAvailableWorkerAndMarksItRunning() {
        JobEntity job = queuedJob();
        WorkerState worker = worker("worker-1");
        when(schedulingPolicyResolver.activePolicy()).thenReturn(schedulingPolicy);
        when(schedulingPolicy.type()).thenReturn(SchedulingPolicyType.LEAST_LOADED);
        when(schedulingPolicy.selectJob()).thenReturn(Optional.of(job));
        when(workerRegistry.list()).thenReturn(List.of(worker));
        when(schedulingPolicy.selectWorker(List.of(worker))).thenReturn(Optional.of(worker));
        when(jobRepository.saveAndFlush(any(JobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean dispatched = jobDispatcher.dispatchOne();

        assertThat(dispatched).isTrue();
        assertThat(job.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.getAssignedWorkerId()).isEqualTo("worker-1");
        assertThat(job.getDispatchAttemptId()).isNotNull();
        verify(workerDispatchClient).dispatch(job, worker);
    }

    @Test
    void leavesQueuedJobUntouchedWhenNoWorkerIsAvailable() {
        JobEntity job = queuedJob();
        when(schedulingPolicyResolver.activePolicy()).thenReturn(schedulingPolicy);
        when(schedulingPolicy.selectJob()).thenReturn(Optional.of(job));
        when(workerRegistry.list()).thenReturn(List.of());
        when(schedulingPolicy.selectWorker(List.of())).thenReturn(Optional.empty());

        boolean dispatched = jobDispatcher.dispatchOne();

        assertThat(dispatched).isFalse();
        verify(workerDispatchClient, never()).dispatch(any(), any());
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

    private static WorkerState worker(String workerId) {
        Instant now = Instant.parse("2026-05-05T10:00:00Z");
        return new WorkerState(
                workerId,
                "worker-service",
                "0.1.0",
                "test",
                URI.create("http://" + workerId + ":8081"),
                4,
                0,
                0.0,
                WorkerStatus.ACTIVE,
                now,
                now,
                now
        );
    }
}
