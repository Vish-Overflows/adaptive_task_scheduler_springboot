package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class JsonTransformWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "JSON_TRANSFORM";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int records = WorkloadPayloads.intValue(payload, "records", 50_000, 1_000, 250_000);
        long checksum = 0L;
        for (int index = 0; index < records; index++) {
            String id = "record-" + index;
            int score = ((index * 31) ^ (index >>> 3)) & 0xFFFF;
            String normalized = id.toUpperCase() + ":" + Integer.toHexString(score);
            checksum += normalized.hashCode();
        }
        return new WorkloadResult("transformed %d synthetic records checksum %d".formatted(records, checksum), records);
    }
}
