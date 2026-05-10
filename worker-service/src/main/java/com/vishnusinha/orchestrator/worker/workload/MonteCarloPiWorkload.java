package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MonteCarloPiWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "MONTE_CARLO_PI";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int samples = WorkloadPayloads.intValue(payload, "samples", 250_000, 1_000, 1_000_000);
        long seed = WorkloadPayloads.intValue(payload, "seed", 7, 0, Integer.MAX_VALUE);
        Random random = new Random(seed);
        int insideCircle = 0;
        for (int index = 0; index < samples; index++) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            if ((x * x) + (y * y) <= 1.0) {
                insideCircle++;
            }
        }
        double piEstimate = 4.0 * insideCircle / samples;
        return new WorkloadResult("pi estimate %.6f".formatted(piEstimate), samples);
    }
}
