package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkloadCatalogTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkloadCatalog workloadCatalog = new WorkloadCatalog(List.of(
            new CpuBenchmarkWorkload(),
            new MatrixMultiplyWorkload(),
            new HashComputeWorkload(),
            new SortBenchmarkWorkload(),
            new MonteCarloPiWorkload(),
            new JsonTransformWorkload(),
            new GraphTraversalWorkload()
    ));

    @Test
    void executesAllSupportedWorkloadTypes() {
        assertWorkload("CPU_BENCHMARK", "{\"upperBound\": 2000}");
        assertWorkload("MATRIX_MULTIPLY", "{\"matrixSize\": 8}");
        assertWorkload("HASH_COMPUTE", "{\"iterations\": 1000}");
        assertWorkload("SORT_BENCHMARK", "{\"itemCount\": 1000}");
        assertWorkload("MONTE_CARLO_PI", "{\"samples\": 1000}");
        assertWorkload("JSON_TRANSFORM", "{\"records\": 1000}");
        assertWorkload("GRAPH_TRAVERSAL", "{\"nodes\": 100, \"edgesPerNode\": 2}");
    }

    private void assertWorkload(String type, String jsonPayload) {
        try {
            WorkloadResult result = workloadCatalog.execute(type, objectMapper.readTree(jsonPayload));

            assertThat(result.summary()).isNotBlank();
            assertThat(result.operations()).isPositive();
        } catch (Exception exception) {
            throw new AssertionError("Expected workload to execute: " + type, exception);
        }
    }
}
