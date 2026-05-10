package com.vishnusinha.orchestrator.scheduler.job;

import com.vishnusinha.orchestrator.scheduler.job.api.JobResponse;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toResponse(JobEntity job) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getPriority(),
                job.getPayload(),
                job.getEstimatedDurationMs(),
                job.getStatus(),
                job.getAssignedWorkerId(),
                job.getScheduledPolicy(),
                job.getRetryCount(),
                job.getIdempotencyKey(),
                job.getCreatedAt(),
                job.getQueuedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getUpdatedAt()
        );
    }
}
