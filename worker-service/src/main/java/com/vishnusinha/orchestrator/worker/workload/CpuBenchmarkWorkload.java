package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class CpuBenchmarkWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "CPU_BENCHMARK";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int upperBound = WorkloadPayloads.intValue(payload, "upperBound", 50_000, 1_000, 200_000);
        int primes = 0;
        for (int candidate = 2; candidate <= upperBound; candidate++) {
            if (isPrime(candidate)) {
                primes++;
            }
        }
        return new WorkloadResult("counted %d primes up to %d".formatted(primes, upperBound), upperBound);
    }

    private static boolean isPrime(int value) {
        if (value < 2) {
            return false;
        }
        for (int divisor = 2; divisor * divisor <= value; divisor++) {
            if (value % divisor == 0) {
                return false;
            }
        }
        return true;
    }
}
