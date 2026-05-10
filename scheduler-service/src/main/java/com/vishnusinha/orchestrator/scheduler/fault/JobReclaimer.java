package com.vishnusinha.orchestrator.scheduler.fault;

import com.vishnusinha.orchestrator.scheduler.config.FaultToleranceProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.job.JobRepository;
import com.vishnusinha.orchestrator.scheduler.job.JobStatus;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerRegistry;
import com.vishnusinha.orchestrator.scheduler.worker.WorkerState;
import com.vishnusinha.orchestrator.shared.worker.WorkerStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JobReclaimer {

    private static final Set<JobStatus> IN_FLIGHT_STATUSES = EnumSet.of(JobStatus.SCHEDULED, JobStatus.RUNNING);

    private final FaultToleranceProperties properties;
    private final JobRepository jobRepository;
    private final WorkerRegistry workerRegistry;
    private final JobFailureHandler jobFailureHandler;
    private final OrchestratorMetrics orchestratorMetrics;
    private final Clock clock;

    public JobReclaimer(
            FaultToleranceProperties properties,
            JobRepository jobRepository,
            WorkerRegistry workerRegistry,
            JobFailureHandler jobFailureHandler,
            OrchestratorMetrics orchestratorMetrics,
            Clock clock
    ) {
        this.properties = properties;
        this.jobRepository = jobRepository;
        this.workerRegistry = workerRegistry;
        this.jobFailureHandler = jobFailureHandler;
        this.orchestratorMetrics = orchestratorMetrics;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${orchestrator.fault-tolerance.scan-interval-ms:5000}",
            fixedDelayString = "${orchestrator.fault-tolerance.scan-interval-ms:5000}"
    )
    public void reclaimTick() {
        if (!properties.enabled()) {
            return;
        }
        reclaimUnsafeJobs();
    }

    @Transactional
    public int reclaimUnsafeJobs() {
        Set<String> unhealthyWorkerIds = workerRegistry.list().stream()
                .filter(worker -> worker.status() == WorkerStatus.UNHEALTHY)
                .map(WorkerState::workerId)
                .collect(Collectors.toSet());

        Instant staleBefore = Instant.now(clock).minus(properties.inFlightTimeout());
        int reclaimed = 0;

        for (JobEntity job : jobRepository.findByStatusIn(IN_FLIGHT_STATUSES)) {
            if (isAssignedToUnhealthyWorker(job, unhealthyWorkerIds)) {
                orchestratorMetrics.recordJobReclaimed(job, "unhealthy_worker");
                jobFailureHandler.retryOrFail(job, "assigned worker is unhealthy");
                reclaimed++;
                continue;
            }

            if (isInFlightTooLong(job, staleBefore)) {
                orchestratorMetrics.recordJobReclaimed(job, "in_flight_timeout");
                jobFailureHandler.retryOrFail(
                        job,
                        "job exceeded in-flight timeout of " + properties.inFlightTimeout()
                );
                reclaimed++;
            }
        }

        return reclaimed;
    }

    private static boolean isAssignedToUnhealthyWorker(JobEntity job, Set<String> unhealthyWorkerIds) {
        return job.getAssignedWorkerId() != null && unhealthyWorkerIds.contains(job.getAssignedWorkerId());
    }

    private static boolean isInFlightTooLong(JobEntity job, Instant staleBefore) {
        Instant referenceTime = job.getStartedAt() != null ? job.getStartedAt() : job.getUpdatedAt();
        return referenceTime != null && referenceTime.isBefore(staleBefore);
    }
}
