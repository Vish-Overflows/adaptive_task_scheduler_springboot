package com.vishnusinha.orchestrator.shared.worker;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkerHeartbeatRequest(
        @NotBlank(message = "workerId is required")
        @Size(max = 120, message = "workerId must be at most 120 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.:-]+$", message = "workerId contains unsupported characters")
        String workerId,

        @Min(value = 0, message = "activeJobCount cannot be negative")
        @Max(value = 1024, message = "activeJobCount must be at most 1024")
        int activeJobCount,

        @Min(value = 1, message = "maxConcurrentJobs must be at least 1")
        @Max(value = 1024, message = "maxConcurrentJobs must be at most 1024")
        int maxConcurrentJobs,

        @DecimalMin(value = "0.0", message = "loadScore must be at least 0")
        @DecimalMax(value = "1.0", message = "loadScore must be at most 1")
        double loadScore
) {
}
