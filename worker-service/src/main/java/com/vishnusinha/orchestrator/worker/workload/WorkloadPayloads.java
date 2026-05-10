package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;

final class WorkloadPayloads {

    private WorkloadPayloads() {
    }

    static int intValue(JsonNode payload, String field, int defaultValue, int min, int max) {
        JsonNode value = payload == null ? null : payload.get(field);
        int parsed = value != null && value.canConvertToInt() ? value.asInt() : defaultValue;
        if (parsed < min || parsed > max) {
            throw new WorkloadExecutionException(
                    "%s must be between %d and %d".formatted(field, min, max)
            );
        }
        return parsed;
    }
}
