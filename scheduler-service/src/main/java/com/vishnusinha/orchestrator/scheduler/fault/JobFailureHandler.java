package com.vishnusinha.orchestrator.scheduler.fault;

import com.vishnusinha.orchestrator.scheduler.config.FaultToleranceProperties;
import com.vishnusinha.orchestrator.scheduler.job.JobEntity;
import com.vishnusinha.orchestrator.scheduler.metrics.OrchestratorMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class JobFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(JobFailureHandler.class);

    private final FaultToleranceProperties properties;
    private final OrchestratorMetrics orchestratorMetrics;
    private final Clock clock;

    public JobFailureHandler(
            FaultToleranceProperties properties,
            OrchestratorMetrics orchestratorMetrics,
            Clock clock
    ) {
        this.properties = properties;
        this.orchestratorMetrics = orchestratorMetrics;
        this.clock = clock;
    }

    public void retryOrFail(JobEntity job, String reason) {
        if (job.getRetryCount() < properties.maxRetries()) {
            job.retryFromFailure(clock);
            orchestratorMetrics.recordJobRetried(job, normalizeReason(reason));
            log.warn(
                    "Retrying job {} after failure: {}. Retry attempt {}/{}.",
                    job.getId(),
                    reason,
                    job.getRetryCount(),
                    properties.maxRetries()
            );
            return;
        }

        job.markFailed(clock);
        orchestratorMetrics.recordJobFailed(job, normalizeReason(reason));
        log.warn("Job {} permanently failed after {} retries: {}", job.getId(), job.getRetryCount(), reason);
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
