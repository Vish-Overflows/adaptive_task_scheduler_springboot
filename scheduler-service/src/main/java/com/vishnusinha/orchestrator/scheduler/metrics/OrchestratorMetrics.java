package com.vishnusinha.orchestrator.scheduler.metrics;

import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.policy.SchedulingPolicyType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrchestratorMetrics {

    private final MeterRegistry meterRegistry;

    public OrchestratorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordJobSubmitted(String jobType, boolean created) {
        Counter.builder("orchestrator_jobs_submitted_total")
                .description("Total job submission attempts")
                .tag("job_type", jobType)
                .tag("created", Boolean.toString(created))
                .register(meterRegistry)
                .increment();
    }

    public void recordJobDispatched(JobEntity job, SchedulingPolicyType policy) {
        Counter.builder("orchestrator_jobs_dispatched_total")
                .description("Total jobs accepted by workers")
                .tag("job_type", job.getType())
                .tag("policy", policy.name())
                .register(meterRegistry)
                .increment();

        if (job.getQueuedAt() != null && job.getStartedAt() != null) {
            Timer.builder("orchestrator_job_queue_wait")
                    .description("Time jobs spend waiting before execution")
                    .tag("job_type", job.getType())
                    .tag("policy", policy.name())
                    .register(meterRegistry)
                    .record(Duration.between(job.getQueuedAt(), job.getStartedAt()));
        }
    }

    public void recordJobCompleted(JobEntity job) {
        Counter.builder("orchestrator_jobs_completed_total")
                .description("Total completed jobs")
                .tag("job_type", job.getType())
                .register(meterRegistry)
                .increment();

        recordDurations(job);
    }

    public void recordJobFailed(JobEntity job, String reason) {
        Counter.builder("orchestrator_jobs_failed_total")
                .description("Total permanently failed jobs")
                .tag("job_type", job.getType())
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordJobRetried(JobEntity job, String reason) {
        Counter.builder("orchestrator_jobs_retried_total")
                .description("Total job retry attempts")
                .tag("job_type", job.getType())
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordJobReclaimed(JobEntity job, String reason) {
        Counter.builder("orchestrator_jobs_reclaimed_total")
                .description("Total jobs reclaimed by the fault monitor")
                .tag("job_type", job.getType())
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    private void recordDurations(JobEntity job) {
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            Timer.builder("orchestrator_job_execution")
                    .description("Worker execution time")
                    .tag("job_type", job.getType())
                    .register(meterRegistry)
                    .record(Duration.between(job.getStartedAt(), job.getCompletedAt()));
        }

        if (job.getCreatedAt() != null && job.getCompletedAt() != null) {
            Timer.builder("orchestrator_job_end_to_end")
                    .description("End-to-end job latency")
                    .tag("job_type", job.getType())
                    .register(meterRegistry)
                    .record(Duration.between(job.getCreatedAt(), job.getCompletedAt()));
        }
    }
}
