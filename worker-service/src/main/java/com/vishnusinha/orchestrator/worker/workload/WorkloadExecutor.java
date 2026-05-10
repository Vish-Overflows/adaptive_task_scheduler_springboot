package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;

public interface WorkloadExecutor {

    String type();

    WorkloadResult execute(JsonNode payload);
}
