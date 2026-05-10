package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Random;

@Component
public class SortBenchmarkWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "SORT_BENCHMARK";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int itemCount = WorkloadPayloads.intValue(payload, "itemCount", 100_000, 1_000, 500_000);
        long seed = WorkloadPayloads.intValue(payload, "seed", 42, 0, Integer.MAX_VALUE);
        int[] values = new int[itemCount];
        Random random = new Random(seed);
        for (int index = 0; index < itemCount; index++) {
            values[index] = random.nextInt();
        }
        Arrays.sort(values);
        long checksum = (long) values[0] + values[itemCount / 2] + values[itemCount - 1];
        return new WorkloadResult("sorted %d integers checksum %d".formatted(itemCount, checksum), itemCount);
    }
}
