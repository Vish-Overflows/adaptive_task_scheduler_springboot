package com.vishnusinha.orchestrator.shared.worker;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.net.URI;

public record RegisterWorkerRequest(
        @NotBlank(message = "workerId is required")
        @Size(max = 120, message = "workerId must be at most 120 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.:-]+$", message = "workerId contains unsupported characters")
        String workerId,

        @NotBlank(message = "serviceName is required")
        @Size(max = 120, message = "serviceName must be at most 120 characters")
        String serviceName,

        @NotBlank(message = "version is required")
        @Size(max = 40, message = "version must be at most 40 characters")
        String version,

        @NotBlank(message = "environment is required")
        @Size(max = 40, message = "environment must be at most 40 characters")
        String environment,

        @NotNull(message = "baseUrl is required")
        URI baseUrl,

        @Min(value = 1, message = "maxConcurrentJobs must be at least 1")
        @Max(value = 1024, message = "maxConcurrentJobs must be at most 1024")
        int maxConcurrentJobs
) {
}
