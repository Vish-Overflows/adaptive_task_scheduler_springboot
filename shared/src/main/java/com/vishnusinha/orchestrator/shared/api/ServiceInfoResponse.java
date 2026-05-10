package com.vishnusinha.orchestrator.shared.api;

import com.vishnusinha.orchestrator.shared.ServiceRole;

import java.time.Instant;

public record ServiceInfoResponse(
        String serviceName,
        ServiceRole role,
        String version,
        String environment,
        Instant serverTime
) {
}
