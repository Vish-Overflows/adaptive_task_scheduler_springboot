package com.vishnusinha.orchestrator.scheduler.job;

import com.vishnusinha.orchestrator.scheduler.fault.JobFailureHandler;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import com.vishnusinha.orchestrator.shared.job.CompleteJobRequest;
import com.vishnusinha.orchestrator.shared.job.JobExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class JobCompletionService {

    private final JobRepository jobRepository;
    private final JobFailureHandler jobFailureHandler;
    private final OrchestratorMetrics orchestratorMetrics;
    private final Clock clock;

    public JobCompletionService(
            JobRepository jobRepository,
            JobFailureHandler jobFailureHandler,
            OrchestratorMetrics orchestratorMetrics,
            Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.jobFailureHandler = jobFailureHandler;
        this.orchestratorMetrics = orchestratorMetrics;
        this.clock = clock;
    }

    @Transactional
    public JobEntity complete(CompleteJobRequest request) {
        JobEntity job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new JobNotFoundException(request.jobId()));

        if (job.getAssignedWorkerId() == null || !job.getAssignedWorkerId().equals(request.workerId())) {
            throw new JobCompletionRejectedException(
                    "Worker %s is not assigned to job %s".formatted(request.workerId(), request.jobId())
            );
        }

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            return job;
        }

        if (job.getStatus() != JobStatus.RUNNING && job.getStatus() != JobStatus.SCHEDULED) {
            throw new JobCompletionRejectedException(
                    "Job %s cannot be completed from status %s".formatted(request.jobId(), job.getStatus())
            );
        }

        if (request.status() == JobExecutionStatus.COMPLETED) {
            job.markCompleted(clock);
            orchestratorMetrics.recordJobCompleted(job);
        } else {
            jobFailureHandler.retryOrFail(job, "worker reported failure: " + safeMessage(request.message()));
        }

        return jobRepository.saveAndFlush(job);
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "no details";
        }
        return message.trim();
    }
}
