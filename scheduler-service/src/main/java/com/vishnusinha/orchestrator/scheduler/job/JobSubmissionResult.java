package com.vishnusinha.orchestrator.scheduler.job;

public record JobSubmissionResult(
        JobEntity job,
        boolean created
) {
}
