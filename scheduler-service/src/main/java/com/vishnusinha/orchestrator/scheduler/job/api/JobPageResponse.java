package com.vishnusinha.orchestrator.scheduler.job.api;

import java.util.List;

public record JobPageResponse(
        List<JobResponse> jobs,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
