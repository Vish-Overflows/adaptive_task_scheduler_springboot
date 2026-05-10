package com.vishnusinha.orchestrator.scheduler.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "orchestrator.dispatch")
public record DispatchProperties(
        boolean enabled,

        @Positive
        long intervalMs,

        @Min(1)
        @Max(100)
        int maxJobsPerTick
) {
    public DispatchProperties {
        if (intervalMs == 0) {
            intervalMs = 1000;
        }
        if (maxJobsPerTick == 0) {
            maxJobsPerTick = 1;
        }
    }
}
