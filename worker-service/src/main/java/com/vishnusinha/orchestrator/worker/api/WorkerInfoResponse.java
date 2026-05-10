package com.vishnusinha.orchestrator.worker.api;

import com.vishnusinha.orchestrator.shared.ServiceRole;

import java.net.URI;
import java.time.Instant;

public record WorkerInfoResponse(
        String serviceName,
        ServiceRole role,
        String workerId,
        String version,
        String environment,
        URI schedulerBaseUrl,
        Instant serverTime
) {
}
