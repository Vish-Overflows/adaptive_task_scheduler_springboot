package com.vishnusinha.orchestrator.scheduler.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "orchestrator.fault-tolerance")
public record FaultToleranceProperties(
        @DefaultValue("true")
        boolean enabled,

        @NotNull
        @DefaultValue("60s")
        Duration inFlightTimeout,

        @Positive
        @DefaultValue("5000")
        long scanIntervalMs,

        @Min(0)
        @DefaultValue("3")
        int maxRetries
) {
}
