package com.vishnusinha.orchestrator.shared.job;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record DispatchJobRequest(
        @NotNull
        UUID jobId,

        @NotBlank
        String type,

        @NotNull
        JsonNode payload,

        @Positive
        long estimatedDurationMs
) {
}
