package com.vishnusinha.orchestrator.shared.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CompleteJobRequest(
        @NotNull
        UUID jobId,

        @NotNull
        UUID dispatchAttemptId,

        @NotBlank
        String workerId,

        @NotNull
        JobExecutionStatus status,

        String message,

        @PositiveOrZero
        long runtimeMs
) {
}
