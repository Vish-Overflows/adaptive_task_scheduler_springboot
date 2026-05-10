package com.vishnusinha.orchestrator.scheduler.job.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitJobRequest(
        @NotBlank(message = "type is required")
        @Size(max = 80, message = "type must be at most 80 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "type may only contain letters, numbers, underscores, and hyphens")
        String type,

        @Min(value = 0, message = "priority must be at least 0")
        @Max(value = 1000, message = "priority must be at most 1000")
        Integer priority,

        @NotNull(message = "payload is required")
        JsonNode payload,

        @NotNull(message = "estimatedDurationMs is required")
        @Min(value = 1, message = "estimatedDurationMs must be positive")
        @Max(value = 86_400_000, message = "estimatedDurationMs cannot exceed 24 hours")
        Long estimatedDurationMs,

        @Size(max = 120, message = "idempotencyKey must be at most 120 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.:-]*$", message = "idempotencyKey contains unsupported characters")
        String idempotencyKey
) {
    public int normalizedPriority() {
        return priority == null ? 100 : priority;
    }
}
